package ch.akop.homesystem.models.states;

import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class NormalState implements State {

    private final AnimationFactory animationFactory;
    private final MessageService messageService;
    private final StateServiceImpl stateService;
    private final DeviceService deviceService;

    private Animation mainDoorOpenAnimation;
    private Thread animationThread;


    @Override
    public void entered() {
        // NOP
    }

    @Override
    public void leave() {
        // NOP
    }

    @Override
    public void event(final Event event) {
        switch (event) {
            case DOOR_OPENED -> startMainDoorOpenAnimation();
            case DOOR_CLOSED -> mainDoorClosed();
            case GOOD_NIGHT_PRESSED -> this.stateService.switchState(SleepState.class);
            case CENTRAL_OFF_PRESSED -> doCentralOff();
        }
    }

    @Override
    public void event(final String buttonName, final int buttonEvent) {
        // NOP on unknown buttons
    }

    @SneakyThrows
    private void doCentralOff() {
        if (this.animationThread != null && this.animationThread.isAlive()) {
            this.animationThread.interrupt();
            // it is possible, that lights can be still on
        }
    }

    private void startMainDoorOpenAnimation() {

        log.info("MAIN-DOOR IS OPENED!");
        this.messageService.sendMessageToUser("Wohnungstür wurde geöffnet.");

        createAnimationIfNotExists();

        if (this.deviceService.getDevicesOfType(Light.class)
                .stream()
                .anyMatch(Light::isOn)) {
            // NOP when any light is on
            return;
        }

        if (this.animationThread == null || !this.animationThread.isAlive()) {
            this.animationThread = new Thread(this.mainDoorOpenAnimation::play);
            this.animationThread.start();
        }
    }

    private void mainDoorClosed() {
        log.info("MAIN-DOOR IS CLOSED!");
    }

    private void createAnimationIfNotExists() {
        if (this.mainDoorOpenAnimation == null) {
            this.mainDoorOpenAnimation = this.animationFactory.buildMainDoorAnimation();
        }
    }
}
