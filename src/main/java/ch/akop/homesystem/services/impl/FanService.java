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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FanService {

    private final DeviceService deviceService;
    private final HomeConfig homeConfig;
    private final MotionSensorService motionSensorService;

    private final Map<HomeConfig.FanControlConfig, Disposable> subscribeMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> waitingToTurnOff = new ConcurrentHashMap<>();

    @EventListener
    public void buttonEventHandler(ButtonPressEvent event) {
        homeConfig.getFans()
                .stream()
                .filter(fanConfig -> !subscribeMap.containsKey(fanConfig))
                .filter(fanConfig -> isButtonEventMatchingFanConfig(event.getButtonName(), event.getButtonEvent(), fanConfig))
                .forEach(triggeredFan -> deviceService.findDeviceByName(triggeredFan.getFan(), SimpleLight.class)
                        .ifPresent(fan -> activateFanConfig(triggeredFan, fan)));
    }

    public void startFan(String name) {
        deviceService.findDeviceByName(name, SimpleLight.class)
                .ifPresent(simpleLight -> simpleLight.turnOn(true));

        var oldSubscription = waitingToTurnOff.get(name);
        if (oldSubscription != null) {
            oldSubscription.dispose();
            waitingToTurnOff.remove(name);
        }
    }

    public void stopFan(String name) {
        waitingToTurnOff.put(name, Observable.timer(10, TimeUnit.MINUTES)
                .take(1)
                .subscribe(aLong -> deviceService.findDeviceByName(name, SimpleLight.class)
                        .ifPresent(simpleLight -> simpleLight.turnOn(false))));
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
                .ifPresent(motionSensorService::requestHigherTimeout);

        Optional.ofNullable(triggeredFan.getTurnOffWhenLightTurnedOff())
                .flatMap(lightName -> deviceService.findDeviceByName(lightName, SimpleLight.class))
                .map(light -> waitUntilLightTurnedOff(light)
                        .subscribe(ignore -> {
                            if (!waitingToTurnOff.containsKey(light.getName())) {
                                fan.turnOn(false);
                            }
                            subscribeMap.remove(triggeredFan);
                        }))
                .ifPresent(subscription -> subscribeMap.put(triggeredFan, subscription));
    }

    private static Observable<Boolean> waitUntilLightTurnedOff(SimpleLight light) {
        return light.getState$()
                .skip(1)
                .filter(isOn -> !isOn)
                .take(1);
    }
}
