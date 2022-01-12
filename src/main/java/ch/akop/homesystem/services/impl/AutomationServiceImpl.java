package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.models.devices.sensor.CloseContactState;
import ch.akop.homesystem.models.states.Event;
import ch.akop.homesystem.services.AutomationService;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "home-automation.sensors")
@Setter
@Getter
public class AutomationServiceImpl implements AutomationService {

    private final AnimationFactory animationFactory;
    private final DeviceService deviceService;
    private final MessageService messageService;
    private final StateServiceImpl stateServiceImpl;
    private final HomeConfig homeConfig;
    private final Map<Class<? extends Device>, List<Device<?>>> knownDevices = new HashMap<>();
    private Animation mainDoorOpenAnimation;

    private String mainDoorName;


    @Override
    @SneakyThrows
    public void discoverNewDevices() {
        this.deviceService.getAllDevices().stream()
                .filter(this::unknownDevice)
                .forEach(this::addDevice);

        this.mainDoorOpenAnimation = this.animationFactory.buildMainDoorAnimation();

        log.info("deCONZ is up!");
    }

    private boolean unknownDevice(final Device<?> device) {
        return !this.knownDevices.computeIfAbsent(device.getClass(), aClass -> new ArrayList<>())
                .contains(device);
    }

    private void addDevice(final Device<?> device) {
        this.knownDevices.get(device.getClass()).add(device);

        if (device instanceof CloseContact closeContact && closeContact.getName().equals(this.mainDoorName)) {
            //noinspection ResultOfMethodCallIgnored
            closeContact.getState$()
                    .skip(0)
                    .distinctUntilChanged()
                    .subscribe(this::mainDoorStateChanged);
        }

        if (device instanceof Button button) {
            //noinspection ResultOfMethodCallIgnored
            button.getEvents$()
                    .subscribe(integer -> this.buttonWasPressed(button.getName(), integer));
        }
    }

    private void buttonWasPressed(final String buttonName, final int buttonEvent) {
        this.stateServiceImpl.triggerEvent(buttonName, buttonEvent);
    }
            

    private void mainDoorStateChanged(final CloseContactState state) {
        if (state == CloseContactState.CLOSED) {
            this.stateServiceImpl.triggerEvent(Event.DOOR_CLOSED);
        } else {
            this.stateServiceImpl.triggerEvent(Event.DOOR_OPENED);
        }
    }
}
