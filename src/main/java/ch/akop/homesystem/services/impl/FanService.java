package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.events.ButtonPressEvent;
import ch.akop.homesystem.persistence.model.config.FanConfig;
import ch.akop.homesystem.persistence.repository.config.FanConfigRepository;
import ch.akop.homesystem.services.DeviceService;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FanService {

    private final DeviceService deviceService;
    private final FanConfigRepository fanConfigRepository;
    private final MotionSensorService motionSensorService;

    private final Map<String, Disposable> subscribeMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> waitingToTurnOff = new ConcurrentHashMap<>();

    @EventListener
    @Transactional
    public void buttonEventHandler(ButtonPressEvent event) {
        fanConfigRepository.findAll()
                .stream()
                .filter(fanConfig -> !subscribeMap.containsKey(fanConfig.getName()))
                .filter(fanConfig -> isButtonEventMatchingFanConfig(event.getButtonName(), event.getButtonEvent(), fanConfig))
                .forEach(triggeredFan -> deviceService.findDeviceByName(triggeredFan.getName(), SimpleLight.class)
                        .ifPresent(fan -> activateFanConfig(triggeredFan, fan)));
    }

    public void startFan(@Nullable FanConfig fanConfig) {
        if (fanConfig == null) {
            return;
        }

        deviceService.findDeviceByName(fanConfig.getName(), SimpleLight.class)
                .ifPresent(simpleLight -> simpleLight.turnOn(true));

        var oldSubscription = waitingToTurnOff.get(fanConfig.getName());
        if (oldSubscription != null) {
            oldSubscription.dispose();
            waitingToTurnOff.remove(fanConfig.getName());
        }
    }

    public void stopFan(@Nullable FanConfig fanConfig) {

        if (fanConfig == null) {
            return;
        }

        waitingToTurnOff.put(fanConfig.getName(), Observable.timer(10, TimeUnit.MINUTES)
                .take(1)
                .subscribe(aLong -> deviceService.findDeviceByName(fanConfig.getName(), SimpleLight.class)
                        .ifPresent(simpleLight -> simpleLight.turnOn(false))));
    }

    private boolean isButtonEventMatchingFanConfig(String buttonName, int buttonEvent, FanConfig fanConfig) {
        return fanConfig.getTriggerByButtonName().equals(buttonName)
                && fanConfig.getTriggerByButtonEvent().equals(buttonEvent);
    }

    private void activateFanConfig(FanConfig triggeredFan, SimpleLight fan) {
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
                            subscribeMap.remove(triggeredFan.getName());
                        }))
                .ifPresent(subscription -> subscribeMap.put(triggeredFan.getName(), subscription));
    }

    private static Observable<Boolean> waitUntilLightTurnedOff(SimpleLight light) {
        return light.getState$()
                .skip(1)
                .filter(isOn -> !isOn)
                .take(1);
    }
}
