package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.actor.Light;
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
        homeConfig.getFans()
                .stream()
                .filter(fanConfig -> !subscribeMap.containsKey(fanConfig))
                .filter(fanConfig -> fanConfig.getButtons().stream().anyMatch(button -> button.getName().equalsIgnoreCase(buttonName)
                        && buttonEvent == button.getButtonEvent()))
                .forEach(triggeredFan -> deviceService.findDeviceByName(triggeredFan.getFan(), Light.class)
                        .ifPresent(fan -> {
                            fan.setOn(true);

                            Optional.ofNullable(triggeredFan.getTurnOffWhenLightTurnedOff())
                                    .flatMap(lightName -> deviceService.findDeviceByName(lightName, Light.class))
                                    .map(light -> light.getState$()
                                            .filter(isOn -> !isOn)
                                            .subscribe(ignore -> {
                                                fan.setOn(false);
                                                subscribeMap.remove(triggeredFan);
                                            }))
                                    .ifPresent(subscription -> subscribeMap.put(triggeredFan, subscription));

                        }));
    }
}
