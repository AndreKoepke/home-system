package ch.akop.homesystem.states;

import ch.akop.homesystem.message.Activatable;
import ch.akop.homesystem.message.MotionSensorReactor;
import ch.akop.homesystem.message.SunsetReactor;
import ch.akop.homesystem.message.WeatherPoster;
import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.models.config.User;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.UserService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.services.impl.RainDetectorService;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import ch.akop.homesystem.util.TimedGateKeeper;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static ch.akop.weathercloud.light.LightUnit.WATT_PER_SQUARE_METER;
import static java.time.temporal.ChronoUnit.MINUTES;

@SuppressWarnings("ResultOfMethodCallIgnored")
@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
public class NormalState extends Activatable implements State {

    public static final Duration DEFAULT_DURATION_ANIMATION_BLOCKER = Duration.of(1, MINUTES);
    public static final BigDecimal THRESHOLD_NOT_TURN_LIGHTS_ON = BigDecimal.valueOf(20);
    private final TimedGateKeeper canStartMainDoorAnimation = new TimedGateKeeper();

    private final AnimationFactory animationFactory;
    private final MessageService messageService;
    private final StateServiceImpl stateService;
    private final DeviceService deviceService;
    private final WeatherService weatherService;
    private final SunsetReactor sunsetReactor;
    private final WeatherPoster weatherPoster;
    private final RainDetectorService rainDetectorService;
    private final UserService userService;
    private final MotionSensorReactor motionSensorReactor;

    private Animation mainDoorOpenAnimation;
    private Thread animationThread;
    private Map<User, Boolean> lastPresenceMap;


    @PostConstruct
    public void reactOnHolidayMessage() {
        //noinspection ResultOfMethodCallIgnored
        this.messageService.getMessages()
                .filter(message -> message.equals("/holiday"))
                .subscribe(ignored -> this.stateService.switchState(HolidayState.class));
    }

    @PostConstruct
    public void listenToTheWeather() {
        this.weatherService.getWeather()
                .map(Weather::getLight)
                .map(light -> light.isBiggerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, WATT_PER_SQUARE_METER))
                .subscribe(this.canStartMainDoorAnimation::setForever);
    }

    private void gotNewPresenceMap(final Map<User, Boolean> presenceMap) {
        presenceMap.forEach((user, isAtHome) -> {
            if (!this.lastPresenceMap.get(user).equals(isAtHome)) {
                this.messageService.sendMessageToMainChannel("%s ist %s".formatted(user.getName(),
                        isAtHome ? "nach Hause gekommen." : "weggegangen"));
            }
        });

        this.lastPresenceMap = presenceMap;
    }

    private boolean compareWithLastAndSkipFirst(final Map<User, Boolean> presenceMap) {
        if (this.lastPresenceMap == null) {
            this.lastPresenceMap = presenceMap;
            return false;
        }

        return true;
    }

    @Override
    public void entered() {
        super.disposeWhenClosed(this.weatherPoster.start());
        super.disposeWhenClosed(this.sunsetReactor.start());
        super.disposeWhenClosed(this.motionSensorReactor.start());

        if (this.rainDetectorService.noRainFor().toDays() > 1) {
            this.messageService.sendMessageToMainChannel("Es hat seit %s Tagen nicht geregnet. Giessen nicht vergessen."
                    .formatted(this.rainDetectorService.noRainFor().toDays()));
        }

        this.lastPresenceMap = null;
        super.disposeWhenClosed(this.userService.getPresenceMap$()
                .filter(this::compareWithLastAndSkipFirst)
                .subscribe(this::gotNewPresenceMap));
    }

    @Override
    public void leave() {
        super.dispose();
    }

    @Override
    public void event(final Event event) {
        switch (event) {
            case DOOR_OPENED -> startMainDoorOpenAnimation();
            case DOOR_CLOSED -> log.info("MAIN-DOOR IS CLOSED!");
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
        this.messageService.sendMessageToMainChannel("Wohnungstür wurde geöffnet.");

        if (!this.canStartMainDoorAnimation.isGateOpen()) {
            return;
        }

        this.canStartMainDoorAnimation.blockFor(DEFAULT_DURATION_ANIMATION_BLOCKER);
        createAnimationIfNotExists();

        if (this.deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .anyMatch(SimpleLight::isOn)) {
            // NOP when any light is on
            return;
        }

        if (this.animationThread == null || !this.animationThread.isAlive()) {
            this.animationThread = new Thread(this.mainDoorOpenAnimation::play);
            this.animationThread.start();
        }
    }

    private void createAnimationIfNotExists() {
        if (this.mainDoorOpenAnimation == null) {
            this.mainDoorOpenAnimation = this.animationFactory.buildMainDoorAnimation();
        }
    }

    @Override
    protected void started() {
        entered();
    }
}
