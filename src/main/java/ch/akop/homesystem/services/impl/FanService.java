package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.events.ButtonPressEvent;
import ch.akop.homesystem.persistence.model.config.FanConfig;
import ch.akop.homesystem.persistence.repository.config.FanConfigRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class FanService {

    private final DeviceService deviceService;
    private final FanConfigRepository fanConfigRepository;
    private final MotionSensorService motionSensorService;

    private final Map<FanConfig, Disposable> subscribeMap = new ConcurrentHashMap<>();
    private final Map<FanConfig, Disposable> waitingToTurnOff = new ConcurrentHashMap<>();

    @ConsumeEvent(value = "home/button", blocking = true)
    @Transactional
    public void buttonEventHandler(ButtonPressEvent event) {
        fanConfigRepository.findAll()
                .stream()
                .filter(fanConfig -> !subscribeMap.containsKey(fanConfig))
                .filter(fanConfig -> isButtonEventMatchingFanConfig(event.getButtonName(), event.getButtonEvent(), fanConfig))
                .forEach(triggeredFan -> activateFanConfig(triggeredFan,
                        deviceService.findDeviceByName(triggeredFan.getName(), SimpleLight.class)
                                .orElseThrow(() -> new NoSuchElementException("Fan %s can not be found".formatted(triggeredFan.getName())))));
    }

    public void startFan(@Nullable FanConfig fanConfig) {
        if (fanConfig == null) {
            return;
        }

        deviceService.findDeviceByName(fanConfig.getName(), SimpleLight.class)
                .ifPresent(SimpleLight::turnOn);

        var oldSubscription = waitingToTurnOff.get(fanConfig);
        if (oldSubscription != null) {
            oldSubscription.dispose();
            waitingToTurnOff.remove(fanConfig);
        }
    }

    public void stopFan(@Nullable FanConfig fanConfig) {

        if (fanConfig == null) {
            return;
        }

        waitingToTurnOff.put(fanConfig, Observable.timer(10, TimeUnit.MINUTES)
                .take(1)
                .map(ignore -> deviceService.findDeviceByName(fanConfig.getTurnOffWhenLightTurnedOff(), SimpleLight.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .subscribe(SimpleLight::turnOff));
    }

    private boolean isButtonEventMatchingFanConfig(String buttonName, int buttonEvent, FanConfig fanConfig) {
        return fanConfig.getTriggerByButtonName().equals(buttonName)
                && fanConfig.getTriggerByButtonEvent().equals(buttonEvent);
    }

    private void activateFanConfig(FanConfig triggeredFan, SimpleLight fan) {
        fan.turnOn();

        Optional.ofNullable(triggeredFan.getIncreaseTimeoutForMotionSensor())
                .ifPresent(motionSensorService::requestHigherTimeout);

        Optional.ofNullable(triggeredFan.getTurnOffWhenLightTurnedOff())
                .map(lightName -> deviceService.findDeviceByName(lightName, SimpleLight.class)
                        .orElseThrow(() -> new NoSuchElementException("Light %s could not be found".formatted(lightName))))
                .map(light -> waitUntilLightTurnedOff(light)
                        .subscribe(ignore -> {
                            if (!waitingToTurnOff.containsKey(triggeredFan)) {
                                fan.turnOff();
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
