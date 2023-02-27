package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.persistence.model.config.MotionSensorConfig;
import ch.akop.homesystem.persistence.repository.config.MotionSensorConfigRepository;
import ch.akop.homesystem.states.NormalState;
import ch.akop.homesystem.states.SleepState;
import io.reactivex.rxjava3.core.Observable;
import io.vertx.core.eventbus.EventBus;
import lombok.RequiredArgsConstructor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

@ApplicationScoped
@RequiredArgsConstructor
public class MotionSensorService {

    private final MotionSensorConfigRepository motionSensorConfigRepository;
    private final DeviceService deviceService;
    private final StateService stateService;
    private final WeatherService weatherService;
    private final EventBus eventBus;
    private final Set<String> sensorsWithHigherTimeout = new HashSet<>();

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @PostConstruct
    @Transactional
    public void init() {
        // TODO restart when config changes
        motionSensorConfigRepository.findAll()
                .forEach(motionSensorConfig -> deviceService.getDevicesOfType(MotionSensor.class)
                        .stream()
                        .filter(motionSensor -> motionSensor.getName().equals(motionSensorConfig.getName()))
                        .findFirst()
                        .orElseThrow()
                        .getIsMoving$()
                        .filter(isMoving -> isMatchingWeather(motionSensorConfig))
                        .filter(isMoving -> isMatchingState(motionSensorConfig))
                        .switchMap(isMoving -> delayWhenNoMovement(isMoving, motionSensorConfig))
                        .distinctUntilChanged()
                        .subscribe(isMoving -> handleMotionEvent(motionSensorConfig, isMoving)));
    }

    public Observable<Boolean> delayWhenNoMovement(Boolean movementDetected, MotionSensorConfig motionSensorConfig) {
        if (Boolean.TRUE.equals(movementDetected)) {
            // don't delay, when movement was detected
            return Observable.just(true);
        }

        // but if not movement detected, then wait
        var timeout = isHigherTimeoutRequested(motionSensorConfig)
                ? motionSensorConfig.getKeepMovingFor().toSeconds() * 3
                : motionSensorConfig.getKeepMovingFor().toSeconds();

        return Observable.just(false)
                .delay(timeout, TimeUnit.SECONDS)
                .switchMap(ignored -> {
                    if (isHigherTimeoutRequested(motionSensorConfig)) {
                        // if a timeout requested while waiting for the old timeout,
                        // then increase the timeout
                        return Observable.just(false).delay(timeout * 2, TimeUnit.SECONDS);
                    }

                    return Observable.just(false);
                });
    }

    public void requestHigherTimeout(String sensorName) {
        sensorsWithHigherTimeout.add(sensorName.toLowerCase());
    }

    public boolean isHigherTimeoutRequested(MotionSensorConfig motionSensorConfig) {
        return sensorsWithHigherTimeout.contains(motionSensorConfig.getName().toLowerCase());
    }

    private void turnLightsOn(List<String> lights) {
        deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> lights.contains(light.getName()))
                .filter(SimpleLight::isCurrentlyOff)
                .forEach(light -> {
                    if (light instanceof DimmableLight dimmable) {
                        if (stateService.getCurrentState() instanceof SleepState) {
                            dimmable.setBrightness(10, Duration.of(10, ChronoUnit.SECONDS));
                        } else {
                            dimmable.setBrightness(100, Duration.of(10, ChronoUnit.SECONDS));
                        }
                    } else {
                        light.turnOn(true);
                    }

                });
    }

    private void turnLightsOff(Collection<String> lights) {
        deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> lights.contains(light.getName()))
                .forEach(light -> light.turnOn(false));
    }

    private void handleMotionEvent(MotionSensorConfig config, boolean isMoving) {
        if (config.getAnimation() == null) {
            handleMotionEventLightsTarget(config, isMoving);
        } else {
            handleMotionEventAnimationTarget(config, isMoving);
        }
    }

    private void handleMotionEventAnimationTarget(MotionSensorConfig config, boolean isMoving) {
        if (isMoving) {
            eventBus.publish("home/playAnimation", config.getAnimation());
        } else {
            turnLightsOff(config.getAnimation().getLights());
        }
    }

    private void handleMotionEventLightsTarget(MotionSensorConfig config, boolean isMoving) {
        if (isMoving) {
            turnLightsOn(config.getLights());
        } else {
            turnLightsOff(config.getLights());
            sensorsWithHigherTimeout.remove(config.getName().toLowerCase());
        }
    }

    private boolean isMatchingWeather(MotionSensorConfig config) {
        if (config.getOnlyTurnOnWhenDarkerAs() == null) {
            return true;
        }

        return weatherService.getWeather()
                .take(1)
                .blockingFirst()
                .getLight()
                .isSmallerThan(config.getOnlyTurnOnWhenDarkerAs(), KILO_LUX);
    }

    private boolean isMatchingState(MotionSensorConfig config) {
        if (config.getOnlyAtNormalState() == null || !config.getOnlyAtNormalState()) {
            return true;
        }

        return stateService.isState(NormalState.class);
    }
}
