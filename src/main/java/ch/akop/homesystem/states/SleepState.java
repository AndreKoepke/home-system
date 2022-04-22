package ch.akop.homesystem.states;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.config.User;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.UserService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
    public static final Random RANDOM = new Random();
    private final List<Disposable> disposeWhenLeaveState = new ArrayList<>();

    private final StateServiceImpl stateService;
    private final MessageService messageService;
    private final DeviceService deviceService;
    private final HomeConfig homeConfig;
    private final WeatherService weatherService;
    private final UserService userService;


    private Disposable timerDoorOpen;
    private Map<User, Boolean> presenceAtBeginning;
    private boolean sleepButtonState;


    @Override
    public void entered() {
        this.messageService.sendMessageToMainChannel("Gute Nacht. Ich mache die Lichter in %dmin aus. Falls ich sofort aufwachen soll, schreibt einfach /aufwachen."
                .formatted(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes()));

        this.disposeWhenLeaveState.add(Observable.timer(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes(), TimeUnit.MINUTES)
                .doOnNext(t -> turnLightsOff())
                .doOnNext(duration -> this.messageService
                        .sendMessageToMainChannel("Schlaft gut. Die Lichter gehen jetzt aus. :)")
                        .sendMessageToMainChannel("Ich lege mich auch hin und stehe um %s wieder auf.".formatted(WAKEUP_TIME)))
                .subscribe());

        this.disposeWhenLeaveState.add(Observable.timer(getDurationToWakeupAsSeconds(), TimeUnit.SECONDS)
                .subscribe(a -> this.stateService.switchState(NormalState.class)));

        this.disposeWhenLeaveState.add(this.messageService.getMessages()
                .filter(message -> message.equalsIgnoreCase("/aufwachen"))
                .subscribe(ignored -> this.stateService.switchState(NormalState.class)));

        this.presenceAtBeginning = this.userService.getPresenceMap$().blockingFirst();
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
        this.deviceService.getDevicesOfType(Group.class)
                .stream()
                .flatMap(group -> group.getScenes().stream())
                .filter(scene -> scene.getName().equals(this.homeConfig.getNightSceneName()))
                .forEach(Scene::activate);
    }

    @Override
    public void leave() {
        this.messageService.sendMessageToMainChannel(POSSIBLE_MORNING_TEXTS.get(RANDOM.nextInt(POSSIBLE_MORNING_TEXTS.size())));

        if (this.weatherService.isActive()) {
            final var weather = this.weatherService.getWeather().blockingFirst();
            this.messageService.sendMessageToMainChannel("Es sind %s und es regnet%s.".formatted(
                    weather.getOuterTemperatur(),
                    weather.getRain().isBiggerThan(BigDecimal.ZERO, MILLIMETER_PER_HOUR) ? "" : " nicht"
            ));
        }

        this.disposeWhenLeaveState.forEach(Disposable::dispose);
        this.disposeWhenLeaveState.clear();

        stopDoorOpenTimer();
        checkPresenceMapWhenLeave();
    }

    public void checkPresenceMapWhenLeave() {
        final var currentPresence = this.userService.getPresenceMap$().blockingFirst();

        if (!currentPresence.equals(this.presenceAtBeginning)) {
            currentPresence.forEach((user, isAtHome) -> {
                if (!this.presenceAtBeginning.get(user).equals(isAtHome)) {
                    this.messageService.sendMessageToMainChannel("In der Nacht ist %s %s".formatted(user.getName(),
                            isAtHome ? "nach Hause gekommen." : "weggegangen."));
                }
            });
        }

        this.presenceAtBeginning = null;
    }

    @Override
    public void event(final Event event) {
        switch (event) {
            case DOOR_CLOSED -> stopDoorOpenTimer();
            case DOOR_OPENED -> startDoorOpenTimer();
            default -> {
                // nop
            }
        }
    }

    @Override
    public void event(final String buttonName, final int buttonEvent) {
        if (buttonName.equals(this.homeConfig.getGoodNightButton().getName())
                && buttonEvent == this.homeConfig.getGoodNightButton().getButtonEvent()) {

            if (!this.sleepButtonState) {
                this.deviceService.getDevicesOfType(Group.class).stream()
                        .flatMap(group -> group.getScenes().stream())
                        .filter(scene -> scene.getName().equals(this.homeConfig.getNightRunSceneName()))
                        .forEach(Scene::activate);
            } else {
                this.turnLightsOff();
            }
            this.sleepButtonState = !this.sleepButtonState;
        }

    }

    private void stopDoorOpenTimer() {
        if (this.timerDoorOpen != null) {
            this.timerDoorOpen.dispose();
            this.timerDoorOpen = null;
        }
    }

    private void startDoorOpenTimer() {
        if (this.timerDoorOpen == null || this.timerDoorOpen.isDisposed()) {
            this.timerDoorOpen = Observable.timer(1, TimeUnit.MINUTES)
                    .subscribe(a -> this.messageService.sendMessageToMainChannel("Die Tür ist jetzt schon länger auf ..."));
        }
    }
}
