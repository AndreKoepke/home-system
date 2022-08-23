package ch.akop.homesystem.states;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.UserService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.services.activatable.Activatable;
import ch.akop.homesystem.services.activatable.SunsetReactor;
import ch.akop.homesystem.services.activatable.WeatherPoster;
import ch.akop.homesystem.services.impl.RainDetectorService;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import ch.akop.homesystem.util.TimedGateKeeper;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
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

    private Animation mainDoorOpenAnimation;
    private Thread animationThread;
    private Map<HomeSystemProperties.User, Boolean> lastPresenceMap;


    @PostConstruct
    public void reactOnHolidayMessage() {
        //noinspection ResultOfMethodCallIgnored
        messageService.getMessages()
                .filter(message -> message.equals("/holiday"))
                .subscribe(ignored -> stateService.switchState(HolidayState.class));
    }

    @PostConstruct
    public void listenToTheWeather() {
        weatherService.getWeather()
                .map(Weather::getLight)
                .map(light -> light.isBiggerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX))
                .subscribe(canStartMainDoorAnimation::setForever);
    }

    private void gotNewPresenceMap(Map<HomeSystemProperties.User, Boolean> presenceMap) {
        presenceMap.forEach((user, isAtHome) -> {
            if (!lastPresenceMap.get(user).equals(isAtHome)) {
                messageService.sendMessageToMainChannel("%s ist %s".formatted(user.getName(),
                        Boolean.TRUE.equals(isAtHome) ? "nach Hause gekommen." : "weggegangen"));
            }
        });

        lastPresenceMap = presenceMap;
    }

    private boolean compareWithLastAndSkipFirst(Map<HomeSystemProperties.User, Boolean> presenceMap) {
        if (lastPresenceMap == null) {
            lastPresenceMap = presenceMap;
            return false;
        }

        return true;
    }

    @Override
    public void entered() {
        super.disposeWhenClosed(weatherPoster.start());
        super.disposeWhenClosed(sunsetReactor.start());

        if (rainDetectorService.noRainFor().toDays() > 1) {
            messageService.sendMessageToMainChannel("Es hat seit %s Tagen nicht geregnet. Giessen nicht vergessen."
                    .formatted(rainDetectorService.noRainFor().toDays()));
        }

        lastPresenceMap = null;
        super.disposeWhenClosed(userService.getPresenceMap$()
                .filter(this::compareWithLastAndSkipFirst)
                .subscribe(this::gotNewPresenceMap));
    }

    @Override
    public void leave() {
        super.dispose();
    }

    @EventListener
    public void event(Event event) {

        if (!(stateService.getCurrentState() instanceof NormalState)) {
            return;
        }

        switch (event) {
            case DOOR_OPENED -> startMainDoorOpenAnimation();
            case DOOR_CLOSED -> log.info("MAIN-DOOR IS CLOSED!");
            case GOOD_NIGHT_PRESSED -> stateService.switchState(SleepState.class);
            case CENTRAL_OFF_PRESSED -> doCentralOff();
        }
    }

    @SneakyThrows
    private void doCentralOff() {
        canStartMainDoorAnimation.blockFor(DEFAULT_DURATION_ANIMATION_BLOCKER);

        if (animationThread != null && animationThread.isAlive()) {
            animationThread.interrupt();
            // it is possible, that lights can be still on
        }
    }

    private void startMainDoorOpenAnimation() {

        log.info("MAIN-DOOR IS OPENED!");
        messageService.sendMessageToMainChannel("Wohnungstür wurde geöffnet.");

        if (!canStartMainDoorAnimation.isGateOpen()) {
            return;
        }

        canStartMainDoorAnimation.blockFor(DEFAULT_DURATION_ANIMATION_BLOCKER);
        createAnimationIfNotExists();

        if (deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .anyMatch(SimpleLight::isCurrentStateIsOn)) {
            // NOP when any light is on
            return;
        }

        if (animationThread == null || !animationThread.isAlive()) {
            animationThread = new Thread(mainDoorOpenAnimation::play);
            animationThread.start();
        }
    }

    private void createAnimationIfNotExists() {
        if (mainDoorOpenAnimation == null) {
            mainDoorOpenAnimation = animationFactory.buildMainDoorAnimation();
        }
    }

    @Override
    protected void started() {
        entered();
    }
}
