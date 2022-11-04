package ch.akop.homesystem.states;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.services.*;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ch.akop.homesystem.util.RandomUtil.pickRandomElement;
import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static java.time.temporal.ChronoUnit.MINUTES;

@RequiredArgsConstructor
@Component
@Lazy
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

    private final StateServiceImpl stateService;
    private final MessageService messageService;
    private final DeviceService deviceService;
    private final HomeSystemProperties homeSystemProperties;
    private final WeatherService weatherService;
    private final UserService userService;
    private final ImageCreatorService imageCreatorService;


    private Disposable timerDoorOpen;
    private Map<HomeSystemProperties.User, Boolean> presenceAtBeginning;
    private boolean sleepButtonState;


    @Override
    public void entered() {
        messageService.sendMessageToMainChannel("Gute Nacht. Ich mache die Lichter in %dmin aus. Falls ich sofort aufwachen soll, schreibt einfach /aufwachen."
                .formatted(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes()));

        disposeWhenLeaveState.add(Observable.timer(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes(), TimeUnit.MINUTES)
                .doOnNext(t -> turnLightsOff())
                .doOnNext(duration -> messageService
                        .sendMessageToMainChannel("Schlaft gut. Die Lichter gehen jetzt aus. :)")
                        .sendMessageToMainChannel("Ich lege mich auch hin und stehe um %s wieder auf.".formatted(WAKEUP_TIME)))
                .subscribe());

        disposeWhenLeaveState.add(Observable.timer(getDurationToWakeupAsSeconds(), TimeUnit.SECONDS)
                .subscribe(a -> stateService.switchState(NormalState.class)));

        disposeWhenLeaveState.add(messageService.getMessages()
                .filter(message -> message.equalsIgnoreCase("/aufwachen"))
                .subscribe(ignored -> stateService.switchState(NormalState.class)));

        presenceAtBeginning = userService.getPresenceMap$().blockingFirst();
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


    public void turnLightsOff() {
        deviceService.getDevicesOfType(Group.class)
                .stream()
                .flatMap(group -> group.getScenes().stream())
                .filter(scene -> scene.getName().equals(homeSystemProperties.getNightSceneName()))
                .forEach(Scene::activate);
    }

    @Override
    public void leave() {
        messageService.sendMessageToMainChannel(pickRandomElement(POSSIBLE_MORNING_TEXTS));
        imageCreatorService.generateAndSendDailyImage();

        if (weatherService.isActive()) {
            var weather = weatherService.getWeather().blockingFirst();
            messageService.sendMessageToMainChannel("Es sind %s und es regnet%s.".formatted(
                    weather.getOuterTemperatur(),
                    weather.getRain().isBiggerThan(BigDecimal.ZERO, MILLIMETER_PER_HOUR) ? "" : " nicht"
            ));
        }

        disposeWhenLeaveState.forEach(Disposable::dispose);
        disposeWhenLeaveState.clear();

        stopDoorOpenTimer();
        checkPresenceMapWhenLeave();
    }

    public void checkPresenceMapWhenLeave() {
        var currentPresence = userService.getPresenceMap$().blockingFirst();

        if (!currentPresence.equals(presenceAtBeginning)) {
            currentPresence.forEach((user, isAtHome) -> {
                if (!presenceAtBeginning.get(user).equals(isAtHome)) {
                    messageService.sendMessageToMainChannel("In der Nacht ist %s %s".formatted(user.getName(),
                            isAtHome ? "nach Hause gekommen." : "weggegangen."));
                }
            });
        }

        presenceAtBeginning = null;
    }

    @EventListener
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
                    .filter(scene -> scene.getName().equals(homeSystemProperties.getNightRunSceneName()))
                    .forEach(Scene::activate);
        } else {
            turnLightsOff();
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
                    .subscribe(a -> messageService.sendMessageToMainChannel("Die Tür ist jetzt schon länger auf ..."));
        }
    }
}
