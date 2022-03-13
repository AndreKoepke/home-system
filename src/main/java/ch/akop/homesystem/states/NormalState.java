package ch.akop.homesystem.states;

import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import ch.akop.homesystem.util.TimedGateKeeper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;

@SuppressWarnings("ResultOfMethodCallIgnored")
@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class NormalState implements State {

    public static final Duration DEFAULT_DURATION_ANIMATION_BLOCKER = Duration.of(1, MINUTES);
    private final TimedGateKeeper canStartMainDoorAnimation = new TimedGateKeeper();

    private final AnimationFactory animationFactory;
    private final MessageService messageService;
    private final StateServiceImpl stateService;
    private final DeviceService deviceService;
    private final WeatherService weatherService;

    private Animation mainDoorOpenAnimation;
    private Thread animationThread;


    @PostConstruct
    public void listenToTheWeather() {
        this.weatherService.getWeather()
                .subscribe(weather -> {
                    if (weather.getLight().baseValue().compareTo(new BigDecimal(20)) > 0) {
                        this.canStartMainDoorAnimation.blockFor(Duration.of(20, MINUTES));
                    }
                });
    }

    @Override
    public void entered() {
        this.canStartMainDoorAnimation.reset();
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
        this.canStartMainDoorAnimation.blockFor(DEFAULT_DURATION_ANIMATION_BLOCKER);

        if (this.animationThread != null && this.animationThread.isAlive()) {
            this.animationThread.interrupt();
            // it is possible, that lights can be still on
        }
    }

    private void startMainDoorOpenAnimation() {

        log.info("MAIN-DOOR IS OPENED!");
        this.messageService.sendMessageToUser("Wohnungstür wurde geöffnet.");

        if (!this.canStartMainDoorAnimation.isGateOpen()) {
            return;
        }

        this.canStartMainDoorAnimation.blockFor(DEFAULT_DURATION_ANIMATION_BLOCKER);
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
