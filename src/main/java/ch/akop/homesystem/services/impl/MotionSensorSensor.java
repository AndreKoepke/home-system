package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.states.SleepState;
import io.reactivex.rxjava3.core.Observable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MotionSensorSensor {

    private final HomeConfig homeConfig;
    private final DeviceService deviceService;
    private final StateServiceImpl stateService;

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    @PostConstruct
    protected void setup() {
        this.homeConfig.getMotionSensors().forEach(motionSensorConfig -> this.deviceService.getDevicesOfType(MotionSensor.class)
                .stream()
                .filter(motionSensor -> motionSensor.getName().equals(motionSensorConfig.getSensor()))
                .findFirst()
                .orElseThrow()
                .getIsMoving$()
                .switchMap(isMoving -> isMoving
                        ? Observable.just(isMoving)
                        : Observable.just(isMoving)
                        .delay(motionSensorConfig.getKeepMovingFor().getSeconds(), TimeUnit.SECONDS))
                .distinctUntilChanged()
                .subscribe(isMoving -> {
                    if (isMoving) {
                        turnLightsOn(motionSensorConfig.getLights());
                    } else {
                        turnLightsOff(motionSensorConfig.getLights());
                    }
                }));
    }

    private void turnLightsOn(List<String> lights) {
        this.deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> lights.contains(light.getName()))
                .forEach(light -> {
                    if (light instanceof DimmableLight dimmable) {
                        if (this.stateService.getCurrentState() instanceof SleepState) {
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
        this.deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> lights.contains(light.getName()))
                .forEach(light -> light.turnOn(false));

    }

}
