package ch.akop.homesystem.models.states;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@Lazy
public class SleepState implements State {

    private final StateServiceImpl stateServiceImpl;
    private final MessageService messageService;
    private final DeviceService deviceService;
    private final HomeConfig homeConfig;

    private Disposable timerTurnLightsOff;
    private Disposable timerLeaveSleepState;
    private Disposable timerDoorOpen;


    @Override
    public void entered() {
        this.messageService.sendMessageToUser("Gute Nacht. Ich mache die Lichter aus.");

        this.timerTurnLightsOff = Observable.timer(5, TimeUnit.MINUTES)
                .subscribe(this::turnLightsOff);

        this.timerLeaveSleepState = Observable.timer(6, TimeUnit.HOURS)
                .subscribe(a -> this.stateServiceImpl.switchState(NormalState.class));
    }


    public void turnLightsOff(final long after) {
        this.messageService.sendMessageToUser("Schlaft gut. :)");

        this.deviceService.getAllLights()
                .stream()
                .filter(light -> !this.homeConfig.getNightLights().contains(light.getName()))
                .forEach(light -> light.setBrightness(0, Duration.of(10, ChronoUnit.MINUTES)));
    }

    @Override
    public void leave() {
        this.messageService.sendMessageToUser("Naaa, gut geschalfen?");
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
        // dont listen to events
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
