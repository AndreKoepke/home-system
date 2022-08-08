package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.models.devices.sensor.CloseContactState;
import ch.akop.homesystem.models.events.ButtonPressEvent;
import ch.akop.homesystem.services.AutomationService;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.states.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "home-automation.sensors")
@Setter
@Getter
public class AutomationServiceImpl implements AutomationService {

    private static final int MARCEL_CONSTANT_SECONDS = 30;

    private final AnimationFactory animationFactory;
    private final DeviceService deviceService;
    private final HomeSystemProperties homeSystemProperties;
    private final ApplicationEventPublisher eventPublisher;
    private Animation mainDoorOpenAnimation;

    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends Device>, List<Device<?>>> knownDevices = new HashMap<>();

    private String mainDoorName;


    @Override
    @SneakyThrows
    public void discoverNewDevices() {
        deviceService.getAllDevices().stream()
                .filter(this::unknownDevice)
                .forEach(this::addDevice);

        mainDoorOpenAnimation = animationFactory.buildMainDoorAnimation();

        log.info("deCONZ is up!");
    }

    private boolean unknownDevice(Device<?> device) {
        return !knownDevices.computeIfAbsent(device.getClass(), aClass -> new ArrayList<>())
                .contains(device);
    }

    private void addDevice(Device<?> device) {
        knownDevices.get(device.getClass()).add(device);

        if (device instanceof CloseContact closeContact && closeContact.getName().equals(mainDoorName)) {
            //noinspection ResultOfMethodCallIgnored
            closeContact.getState$()
                    .skip(0)
                    .distinctUntilChanged()
                    .throttleLatest(MARCEL_CONSTANT_SECONDS, TimeUnit.SECONDS)
                    .subscribe(this::mainDoorStateChanged);
        }

        if (device instanceof Button button) {
            //noinspection ResultOfMethodCallIgnored
            button.getEvents$()
                    .subscribe(integer -> buttonWasPressed(button.getName(), integer));
        }
    }

    private void buttonWasPressed(String buttonName, int buttonEvent) {
        if (wasCentralOffPressed(buttonName, buttonEvent)) {
            eventPublisher.publishEvent(Event.CENTRAL_OFF_PRESSED);
        } else if (wasGoodNightButtonPressed(buttonName, buttonEvent)) {
            eventPublisher.publishEvent(Event.GOOD_NIGHT_PRESSED);
        } else {
            eventPublisher.publishEvent(new ButtonPressEvent(buttonName, buttonEvent));
        }
    }


    private boolean wasGoodNightButtonPressed(String buttonName, int buttonEvent) {
        var properties = homeSystemProperties.getGoodNightButton();
        if (properties == null) {
            return false;
        }

        return buttonName.equals(properties.getName()) && buttonEvent == properties.getButtonEvent();
    }

    private boolean wasCentralOffPressed(String buttonName, int buttonEvent) {
        return homeSystemProperties.getCentralOffSwitches().stream()
                .anyMatch(offButton -> offButton.getName().equals(buttonName)
                        && offButton.getButtonEvent() == buttonEvent);
    }


    private void mainDoorStateChanged(CloseContactState state) {
        if (state == CloseContactState.CLOSED) {
            eventPublisher.publishEvent(Event.DOOR_CLOSED);
        } else {
            eventPublisher.publishEvent(Event.DOOR_OPENED);
        }
    }
}
