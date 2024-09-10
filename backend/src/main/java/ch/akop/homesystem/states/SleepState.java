package ch.akop.homesystem.states;

import static ch.akop.homesystem.util.EventConstants.GENERAL;
import static ch.akop.homesystem.util.RandomUtil.pickRandomElement;
import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static java.time.temporal.ChronoUnit.MINUTES;

import ch.akop.homesystem.external.openai.OpenAIService;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.ImageCreatorService;
import ch.akop.homesystem.services.impl.StateService;
import ch.akop.homesystem.services.impl.TelegramMessageService;
import ch.akop.homesystem.services.impl.UserService;
import ch.akop.homesystem.services.impl.WeatherService;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Startup
@RequiredArgsConstructor
@ApplicationScoped
public class SleepState implements State {

  private static final List<String> POSSIBLE_MORNING_TEXTS = List.of("Naaa, gut geschlafen?",
      "Halli Hallo Hallöchen",
      "Guten Morgen liebe Sorgen, seid ihr auch schon alle wach?",
      "Was habt ihr geträumt? Ich hab geträumt, dass überall 0en und 1en waren. 0101110110101000010111010",
      "Hi :)",
      "Guete morgen mitenand.",
      "Bin ich müüüüüüüüüüde. 🥱");

  private static final Duration DURATION_UNTIL_SWITCH_LIGHTS_OFF = Duration.of(3, MINUTES);
  private static final LocalTime WAKEUP_TIME = LocalTime.of(7, 0);
  private final List<Disposable> disposeWhenLeaveState = new ArrayList<>();

  private final StateService stateService;
  private final TelegramMessageService messageService;
  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final UserService userService;
  private final BasicConfigRepository basicConfigRepository;
  private final ImageCreatorService imageCreatorService;
  private final OpenAIService openAIService;
  private final Vertx vertx;

  private Scheduler rxScheduler;
  private Disposable timerDoorOpen;
  private Map<String, Boolean> presenceAtBeginning;
  private boolean sleepButtonState;
  private String nightSceneName;


  void registerState(@Observes @Priority(100) StartupEvent startupEvent) {
    stateService.registerState(SleepState.class, this);
  }

  @PostConstruct
  @Transactional
  void loadNightSceneName() {
    rxScheduler = RxHelper.blockingScheduler(vertx, false);

    basicConfigRepository.findByOrderByModifiedDesc()
        .map(BasicConfig::getNightSceneName)
        .ifPresentOrElse(
            nightSceneName -> this.nightSceneName = nightSceneName,
            () -> log.warn("No nightSceneName set")
        );
  }

  @Override
  public void entered(boolean quiet) {
    if (!quiet) {
      messageService.sendMessageToMainChannel(("Gute Nacht. Ich mache die Lichter in %dmin aus. " +
          "Falls ich sofort aufwachen soll, schreibt einfach /aufwachen.")
          .formatted(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes()));

      disposeWhenLeaveState.add(Observable.timer(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes(), TimeUnit.MINUTES)
          .doOnNext(t -> deviceService.activeSceneForAllGroups(nightSceneName))
          .doOnNext(duration -> messageService
              .sendFunnyMessageToMainChannel("Schlaft gut. Die Lichter gehen jetzt aus. :)")
              .sendFunnyMessageToMainChannel("Ich lege mich auch hin und stehe um %s wieder auf.".formatted(WAKEUP_TIME)))
          .subscribe());
    }

    disposeWhenLeaveState.add(Observable.timer(getDurationToWakeupAsSeconds(), TimeUnit.SECONDS)
        .subscribeOn(rxScheduler)
        .subscribe(a -> stateService.switchState(NormalState.class)));

    disposeWhenLeaveState.add(messageService.getMessages()
        .filter(message -> message.startsWith("/aufwachen"))
        .subscribeOn(rxScheduler)
        .subscribe(ignored -> stateService.switchState(NormalState.class)));

    userService.getPresenceMap$()
        .take(1)
        .subscribe(newPresenceMap -> presenceAtBeginning = newPresenceMap);
  }

  private long getDurationToWakeupAsSeconds() {
    return Duration.between(ZonedDateTime.now(), getWakeUpDateTime()).toSeconds();
  }

  private ZonedDateTime getWakeUpDateTime() {

    if (LocalTime.now().isBefore(WAKEUP_TIME)) {
      return ZonedDateTime.of(LocalDate.now(), WAKEUP_TIME, ZoneId.systemDefault());
    }

    return ZonedDateTime.of(LocalDate.now().plusDays(1), WAKEUP_TIME, ZoneId.systemDefault());
  }


  @Override
  @Transactional
  public void leave() {
    messageService.sendFunnyMessageToMainChannel("Schreibe in eigenen Worten: " + pickRandomElement(POSSIBLE_MORNING_TEXTS));

    if (weatherService.isActive()) {
      //noinspection ResultOfMethodCallIgnored
      weatherService.getWeather()
          .take(1)
          .timeout(10, TimeUnit.SECONDS)
          .subscribe(weather -> messageService.sendMessageToMainChannel("Es sind %s und es regnet%s.".formatted(
              weather.getOuterTemperatur(),
              weather.getRain().isBiggerThan(BigDecimal.ZERO, MILLIMETER_PER_HOUR) ? "" : " nicht"
          )));
    }

    disposeWhenLeaveState.forEach(Disposable::dispose);
    disposeWhenLeaveState.clear();

    stopDoorOpenTimer();
    checkPresenceMapWhenLeave();

    tellJoke();
    imageCreatorService.generateAndSendDailyImage();
  }

  private void tellJoke() {
    var joke = openAIService.requestText("Erzähle einen lustigen Witz.");
    messageService.sendMessageToMainChannel("Witz des Tages: \n" + joke);
  }

  public void checkPresenceMapWhenLeave() {
    var currentPresence = userService.getPresenceMap$().blockingFirst();

    if (!currentPresence.equals(presenceAtBeginning)) {
      currentPresence.forEach((user, isAtHome) -> {
        if (!presenceAtBeginning.get(user).equals(isAtHome)) {
          messageService.sendFunnyMessageToMainChannel("In der Nacht ist %s %s".formatted(user,
              Boolean.TRUE.equals(isAtHome) ? "nach Hause gekommen." : "weggegangen."));
        }
      });
    }

    presenceAtBeginning = null;
  }

  @Transactional
  @ConsumeEvent(value = GENERAL, blocking = true)
  public void event(Event event) {

    if (!(stateService.getCurrentState() instanceof SleepState)) {
      return;
    }

    switch (event) {
      case DOOR_CLOSED -> stopDoorOpenTimer();
      case DOOR_OPENED -> startDoorOpenTimer();
      case GOOD_NIGHT_PRESSED -> doNightRun();
      default -> {
        // nop
      }
    }
  }

  public void doNightRun() {
    if (!sleepButtonState) {
      deviceService.getDevicesOfType(Group.class).stream()
          .flatMap(group -> group.getScenes().stream())
          .filter(scene -> scene.getName().equals(basicConfigRepository.findByOrderByModifiedDesc()
              .orElseThrow()
              .getNightRunSceneName()))
          .forEach(Scene::activate);
    } else {
      deviceService.activeSceneForAllGroups(nightSceneName);
    }
    sleepButtonState = !sleepButtonState;
  }

  private void stopDoorOpenTimer() {
    if (timerDoorOpen != null) {
      timerDoorOpen.dispose();
      timerDoorOpen = null;
    }
  }

  private void startDoorOpenTimer() {
    if (timerDoorOpen == null || timerDoorOpen.isDisposed()) {
      timerDoorOpen = Observable.timer(1, TimeUnit.MINUTES)
          .subscribe(a -> messageService.sendFunnyMessageToMainChannel("Die Tür ist jetzt schon länger auf ..."));
    }
  }
}
