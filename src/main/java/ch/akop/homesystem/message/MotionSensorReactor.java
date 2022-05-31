package ch.akop.homesystem.message;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.services.DeviceService;
import io.reactivex.rxjava3.core.Observable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MotionSensorReactor extends Activatable {

    private final HomeConfig homeConfig;
    private final DeviceService deviceService;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void started() {
        this.homeConfig.getMotionSensors().forEach(motionSensorConfig -> super
                .disposeWhenClosed(this.deviceService.getDevicesOfType(MotionSensor.class)
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
                        })));
    }

    private void turnLightsOn(final List<String> lights) {
        this.deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> lights.contains(light.getName()))
                .forEach(light -> {
                    if (light instanceof DimmableLight dimmable) {
                        dimmable.setBrightness(100, Duration.of(10, ChronoUnit.SECONDS));
                    } else {
                        light.turnOn(true);
                    }

                });
    }

    private void turnLightsOff(final List<String> lights) {
        this.deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> lights.contains(light.getName()))
                .forEach(light -> light.turnOn(false));

    }

}
