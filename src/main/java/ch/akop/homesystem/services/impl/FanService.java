package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.DeviceService;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FanService {

    private final DeviceService deviceService;
    private final HomeConfig homeConfig;

    private final Map<HomeConfig.FanControlConfig, Disposable> subscribeMap = new ConcurrentHashMap<>();

    public void buttonEventHandler(String buttonName, int buttonEvent) {
        this.homeConfig.getFans()
                .stream()
                .filter(fanConfig -> !this.subscribeMap.containsKey(fanConfig))
                .filter(fanConfig -> isButtonEventMatchingFanConfig(buttonName, buttonEvent, fanConfig))
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

        Optional.ofNullable(triggeredFan.getTurnOffWhenLightTurnedOff())
                .flatMap(lightName -> this.deviceService.findDeviceByName(lightName, SimpleLight.class))
                .map(light -> light.getState$()
                        .skip(1)
                        .filter(isOn -> !isOn)
                        .take(1)
                        .subscribe(ignore -> {
                            fan.turnOn(false);
                            this.subscribeMap.remove(triggeredFan);
                        }))
                .ifPresent(subscription -> this.subscribeMap.put(triggeredFan, subscription));
    }
}
