package ch.akop.homesystem.models.states;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MINUTES;

@RequiredArgsConstructor
@Component
@Lazy
public class SleepState implements State {

    private static final List<String> POSSIBLE_MORNING_TEXTS = List.of("Naaa, gut geschlafen?",
            "Halli Hallo Hallöchen",
            "Guten Morgen liebe Sorgen, seid ihr auch schon alle wach?",
            "Was hast du geträumt? Ich hab geträumt, dass überall 0en und 1en waren. 0101110110101000010111010",
            "Hi :)");

    private static final Duration DURATION_UNTIL_SWITCH_LIGHTS_OFF = Duration.of(3, MINUTES);
    private static final LocalTime WAKEUP_TIME = LocalTime.of(7, 0);
    public static final Random RANDOM = new Random();

    private final StateServiceImpl stateService;
    private final MessageService messageService;
    private final DeviceService deviceService;
    private final HomeConfig homeConfig;

    private Disposable timerTurnLightsOff;
    private Disposable timerLeaveSleepState;
    private Disposable timerDoorOpen;
    private boolean sleepButtonState;


    @Override
    public void entered() {
        this.messageService.sendMessageToUser("Gute Nacht. Ich mache die Lichter in %dmin aus."
                .formatted(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes()));

        this.timerTurnLightsOff = Observable.timer(DURATION_UNTIL_SWITCH_LIGHTS_OFF.toMinutes(), TimeUnit.MINUTES)
                .doOnNext(t -> turnLightsOff())
                .doOnNext(duration -> this.messageService
                        .sendMessageToUser("Schlaft gut. Die Lichter gehen jetzt aus. :)")
                        .sendMessageToUser("Ich lege mich auch hin und stehe um %s wieder auf.".formatted(WAKEUP_TIME)))
                .subscribe();

        this.timerLeaveSleepState = Observable.timer(getDurationToWakeupAsSeconds(), TimeUnit.SECONDS)
                .subscribe(a -> this.stateService.switchState(NormalState.class));
    }

    private long getDurationToWakeupAsSeconds() {
        return Duration.between(getWakeUpDateTime(), LocalDateTime.now()).toSeconds();
    }

    private LocalDateTime getWakeUpDateTime() {
        return LocalDateTime.of(LocalDate.now().plusDays(1), WAKEUP_TIME);
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
        this.messageService.sendMessageToUser(POSSIBLE_MORNING_TEXTS.get(RANDOM.nextInt(POSSIBLE_MORNING_TEXTS.size())));
        this.timerTurnLightsOff.dispose();
        this.timerLeaveSleepState.dispose();

        stopDoorOpenTimer();
    }

    @Override
    public void event(final Event event) {
        switch (event) {
            case DOOR_CLOSED -> stopDoorOpenTimer();
            case DOOR_OPENED -> startDoorOpenTimer();
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
                    .subscribe(a -> this.messageService.sendMessageToUser("Die Tür ist jetzt schon länger auf ..."));
        }
    }
}
