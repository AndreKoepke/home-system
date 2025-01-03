package ch.akop.homesystem.states;

import static ch.akop.homesystem.services.impl.UserService.KEEP_CHECKING_FOR;
import static ch.akop.homesystem.util.EventConstants.CUBE;
import static ch.akop.homesystem.util.EventConstants.GENERAL;
import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static java.time.temporal.ChronoUnit.MINUTES;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.events.CubeEvent;
import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.persistence.model.config.CubeConfig;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.persistence.repository.config.CubeConfigRepository;
import ch.akop.homesystem.services.activatable.Activatable;
import ch.akop.homesystem.services.activatable.SunsetReactor;
import ch.akop.homesystem.services.activatable.WeatherPoster;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.RainDetectorService;
import ch.akop.homesystem.services.impl.StateService;
import ch.akop.homesystem.services.impl.TelegramMessageService;
import ch.akop.homesystem.services.impl.UserService;
import ch.akop.homesystem.services.impl.WeatherService;
import ch.akop.homesystem.util.TimedGateKeeper;
import ch.akop.weathercloud.Weather;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.eventbus.EventBus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@SuppressWarnings("ResultOfMethodCallIgnored")
@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
@Startup
public class NormalState extends Activatable implements State {

  public static final Duration DEFAULT_DURATION_ANIMATION_BLOCKER = Duration.of(1, MINUTES);
  public static final BigDecimal THRESHOLD_NOT_TURN_LIGHTS_ON = BigDecimal.valueOf(20);
  private final TimedGateKeeper canStartMainDoorAnimation = new TimedGateKeeper();

  private final TelegramMessageService messageService;
  private final StateService stateService;
  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final SunsetReactor sunsetReactor;
  private final WeatherPoster weatherPoster;
  private final RainDetectorService rainDetectorService;
  private final UserService userService;
  private final BasicConfigRepository basicConfigRepository;
  private final CubeConfigRepository cubeConfigRepository;
  private final EventBus eventBus;
  private Map<String, Boolean> lastPresenceMap;


  void registerState(@Observes @Priority(100) StartupEvent startupEvent) {
    stateService.registerState(NormalState.class, this);
  }


  @PostConstruct
  void listenToTheWeather() {
    weatherService.getWeather()
        .map(Weather::getLight)
        .map(light -> light.isBiggerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX))
        .subscribe(canStartMainDoorAnimation::setForever);
  }

  private void gotNewPresenceMap(Map<String, Boolean> presenceMap) {
    presenceMap.forEach((user, isAtHome) -> {
      if (!lastPresenceMap.get(user).equals(isAtHome)) {
        messageService.sendFunnyMessageToMainChannel("%s ist %s".formatted(user,
            Boolean.TRUE.equals(isAtHome) ? "nach Hause gekommen." : "weggegangen"));
      }
    });

    lastPresenceMap = presenceMap;
  }

  private boolean compareWithLastAndSkipFirst(Map<String, Boolean> presenceMap) {
    if (lastPresenceMap == null) {
      lastPresenceMap = presenceMap;
      return false;
    }

    return true;
  }

  @Override
  public void entered(boolean quiet) {
    super.disposeWhenClosed(weatherPoster.start());
    super.disposeWhenClosed(sunsetReactor.start());

    if (!quiet && rainDetectorService.noRainFor().toDays() > 1) {
      messageService.sendFunnyMessageToMainChannel("Es hat seit %s Tagen nicht geregnet. Giessen nicht vergessen."
          .formatted(rainDetectorService.noRainFor().toDays()));
    }

    lastPresenceMap = null;
    super.disposeWhenClosed(userService.getPresenceMap$()
        .filter(this::compareWithLastAndSkipFirst)
        .skip(quiet ? KEEP_CHECKING_FOR.toSeconds() : 0, TimeUnit.SECONDS)
        .subscribe(this::gotNewPresenceMap));

    super.disposeWhenClosed(userService.isAnyoneAtHome$()
        .delay(10, TimeUnit.MINUTES)
        .switchMap(this::shouldLightsTurnedOff)
        .filter(canTurnOff -> canTurnOff)
        .subscribe(canTurnOff -> deviceService.turnAllLightsOff()));

    super.disposeWhenClosed(messageService.waitForMessageOnce("sleep")
        .subscribe(message -> {
          messageService.sendFunnyMessageToMainChannel("Ok, gute Nacht.");
          stateService.activateStateQuietly(SleepState.class);
        }));

    super.disposeWhenClosed(messageService.waitForMessageOnce("holiday")
        .subscribe(ignored -> stateService.switchState(HolidayState.class)));
  }

  @Override
  public void leave() {
    super.dispose();
  }

  @ConsumeEvent(value = GENERAL, blocking = true)
  public void event(Event event) {

    if (!(stateService.getCurrentState() instanceof NormalState)) {
      return;
    }

    switch (event) {
      case DOOR_OPENED -> startMainDoorOpenAnimation();
      case DOOR_CLOSED -> log.info("MAIN-DOOR IS CLOSED!");
      case GOOD_NIGHT_PRESSED -> stateService.switchState(SleepState.class);
      case CENTRAL_OFF_PRESSED -> canStartMainDoorAnimation.blockFor(DEFAULT_DURATION_ANIMATION_BLOCKER);
    }
  }

  @Transactional
  @ConsumeEvent(value = CUBE, blocking = true)
  public void event(CubeEvent cubeEvent) {
    cubeConfigRepository.findById(cubeEvent.getCubeName())
        .ifPresent(cubeConfig -> {
          switch (cubeEvent.getEventType()) {
            case FLIPPED_TO_SIDE_1 -> activeDeviceOrScene(cubeConfig.getSceneNameOnSide_1(), cubeConfig.getDeviceNameOnSide_1());
            case FLIPPED_TO_SIDE_2 -> activeDeviceOrScene(cubeConfig.getSceneNameOnSide_2(), cubeConfig.getDeviceNameOnSide_2());
            case FLIPPED_TO_SIDE_3 -> activeDeviceOrScene(cubeConfig.getSceneNameOnSide_3(), cubeConfig.getDeviceNameOnSide_3());
            case FLIPPED_TO_SIDE_4 -> activeDeviceOrScene(cubeConfig.getSceneNameOnSide_4(), cubeConfig.getDeviceNameOnSide_4());
            case FLIPPED_TO_SIDE_5 -> activeDeviceOrScene(cubeConfig.getSceneNameOnSide_5(), cubeConfig.getDeviceNameOnSide_5());
            case FLIPPED_TO_SIDE_6 -> activeDeviceOrScene(cubeConfig.getSceneNameOnSide_6(), cubeConfig.getDeviceNameOnSide_5());
            case SHAKED -> shakeHandle(cubeConfig);
          }
        });
  }

  private void shakeHandle(CubeConfig config) {
    if (StringUtils.hasText(config.getSceneNameOnShake())) {
      deviceService.activeSceneForAllGroups(config.getSceneNameOnShake());
    }
  }

  private void activeDeviceOrScene(String sceneName, String deviceName) {
    if (StringUtils.hasText(sceneName)) {
      deviceService.activeSceneForAllGroups(sceneName);
    }

    if (StringUtils.hasText(deviceName)) {
      var light = deviceService.findDeviceByName(deviceName, SimpleLight.class)
          .orElseThrow(() -> new NoSuchElementException("No light named " + deviceName + " found"));

      light.turnOn();
    }
  }

  private Flowable<Boolean> shouldLightsTurnedOff(boolean anyOneAtHome) {

    if (anyOneAtHome || !deviceService.isAnyLightOn()) {
      return Flowable.just(false);
    }

    messageService.sendMessageToMainChannel("Es niemand zu Hause, deswegen mache ich gleich die Lichter aus." +
        "Es sei denn, /lassAn");

    return messageService.waitForMessageOnce("lassAn")
        .doOnNext(message -> messageService.sendMessageToMainChannel("Ok, ich lasse die Lichter an."))
        .map(s -> false)
        .timeout(5, TimeUnit.MINUTES)
        .onErrorReturn(throwable -> !userService.isAnyoneAtHome())
        .toFlowable(BackpressureStrategy.LATEST);
  }

  private void startMainDoorOpenAnimation() {
    log.info("MAIN-DOOR IS OPENED!");
    messageService.sendFunnyMessageToMainChannel("Wohnungstür wurde geöffnet.");

    if (!canStartMainDoorAnimation.isGateOpen() || deviceService.isAnyLightOn()) {
      return;
    }

    canStartMainDoorAnimation.setForever(true);
    try {
      eventBus.publish("home/animation/play", basicConfigRepository.findByOrderByModifiedDesc()
          .orElseThrow()
          .getWhenMainDoorOpened());

    } finally {
      canStartMainDoorAnimation.reset();
    }
  }

  @Override
  protected void started() {
    entered(true);
  }
}
