package ch.akop.homesystem.models.states;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.services.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Lazy
@Component
@RequiredArgsConstructor
public class NormalState implements State {

    private final AnimationFactory animationFactory;
    private final MessageService messageService;
    private final HomeConfig homeConfig;

    private Animation mainDoorOpenAnimation;
    private Thread animationThread;


    @Override
    public void entered() {
        // just nothing
    }

    @Override
    public void leave() {
        // just nothing
    }

    @Override
    public void event(final Event event) {

        switch (event) {
            case DOOR_OPENED -> startMainDoorOpenAnimation();
            case DOOR_CLOSED -> mainDoorClosed();
        }
    }

    @Override
    public void event(final String buttonName, final int buttonEvent) {
        this.homeConfig.getCentralOffSwitches().stream()
                .filter(offButton -> buttonName.equals(offButton.getName()) && offButton.getButtonEvent() == buttonEvent)
                .findAny()
                .ifPresent(offButton -> {
                    if (this.animationThread != null && this.animationThread.isAlive()) {
                        this.animationThread.interrupt();
                    }
                });
    }

    private void startMainDoorOpenAnimation() {

        createAnimationIfNotExists();

        if (this.animationThread == null || !this.animationThread.isAlive()) {
            this.animationThread = new Thread(this.mainDoorOpenAnimation::play);
            this.animationThread.start();
        }
        log.info("MAIN-DOOR IS OPENED!");
        this.messageService.sendMessageToUser("Wohnungstür wurde geöffnet.");
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
