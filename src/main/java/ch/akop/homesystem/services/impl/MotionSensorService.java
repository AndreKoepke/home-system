package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.states.SleepState;
import io.reactivex.rxjava3.core.Observable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MotionSensorService {

    private final HomeSystemProperties homeSystemProperties;
    private final DeviceService deviceService;
    private final StateServiceImpl stateService;
    private final Set<String> sensorsWithHigherTimeout = new HashSet<>();

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @PostConstruct
    protected void setup() {
        homeSystemProperties.getMotionSensors().forEach(motionSensorConfig -> deviceService.getDevicesOfType(MotionSensor.class)
                .stream()
                .filter(motionSensor -> motionSensor.getName().equals(motionSensorConfig.getSensor()))
                .findFirst()
                .orElseThrow()
                .getIsMoving$()
                .switchMap(isMoving -> delayWhenNoMovement(isMoving, motionSensorConfig))
                .distinctUntilChanged()
                .subscribe(isMoving -> {
                    if (Boolean.TRUE.equals(isMoving)) {
                        turnLightsOn(motionSensorConfig.getLights());
                    } else {
                        turnLightsOff(motionSensorConfig.getLights());
                        sensorsWithHigherTimeout.remove(motionSensorConfig.getSensor().toLowerCase());
                    }
                }));
    }

    public Observable<Boolean> delayWhenNoMovement(Boolean movementDetected, HomeSystemProperties.MotionSensorConfig motionSensorConfig) {
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

    public boolean isHigherTimeoutRequested(HomeSystemProperties.MotionSensorConfig motionSensorConfig) {
        return sensorsWithHigherTimeout.contains(motionSensorConfig.getSensor().toLowerCase());
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

    private void turnLightsOff(List<String> lights) {
        deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> lights.contains(light.getName()))
                .forEach(light -> light.turnOn(false));

    }

}
