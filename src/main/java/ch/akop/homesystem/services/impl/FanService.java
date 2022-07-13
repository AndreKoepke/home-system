package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.events.ButtonPressEvent;
import ch.akop.homesystem.services.DeviceService;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FanService {

    private final DeviceService deviceService;
    private final HomeConfig homeConfig;
    private final MotionSensorService motionSensorService;

    private final Map<HomeConfig.FanControlConfig, Disposable> subscribeMap = new ConcurrentHashMap<>();

    @EventListener
    public void buttonEventHandler(ButtonPressEvent event) {
        this.homeConfig.getFans()
                .stream()
                .filter(fanConfig -> !this.subscribeMap.containsKey(fanConfig))
                .filter(fanConfig -> isButtonEventMatchingFanConfig(event.getButtonName(), event.getButtonEvent(), fanConfig))
                .forEach(triggeredFan -> this.deviceService.findDeviceByName(triggeredFan.getFan(), SimpleLight.class)
                        .ifPresent(fan -> activateFanConfig(triggeredFan, fan)));
    }

    private boolean isButtonEventMatchingFanConfig(String buttonName, int buttonEvent, HomeConfig.FanControlConfig fanConfig) {
        return fanConfig.getButtons()
                .stream()
                .anyMatch(button -> button.getName().equalsIgnoreCase(buttonName)
                        && buttonEvent == button.getButtonEvent());
    }

    private void activateFanConfig(HomeConfig.FanControlConfig triggeredFan, SimpleLight fan) {
        fan.turnOn(true);

        Optional.ofNullable(triggeredFan.getIncreaseTimeoutForMotionSensor())
                .ifPresent(this.motionSensorService::requestHigherTimeout);

        Optional.ofNullable(triggeredFan.getTurnOffWhenLightTurnedOff())
                .flatMap(lightName -> this.deviceService.findDeviceByName(lightName, SimpleLight.class))
                .map(light -> waitUntilLightTurnedOff(light)
                        .subscribe(ignore -> {
                            fan.turnOn(false);
                            this.subscribeMap.remove(triggeredFan);
                        }))
                .ifPresent(subscription -> this.subscribeMap.put(triggeredFan, subscription));
    }

    private static Observable<Boolean> waitUntilLightTurnedOff(SimpleLight light) {
        return light.getState$()
                .skip(1)
                .filter(isOn -> !isOn)
                .take(1);
    }
}
