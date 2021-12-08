package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.animation.Animation;
import ch.akop.homesystem.models.animation.AnimationFactory;
import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.models.devices.sensor.CloseContactState;
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
public class AutomationServiceImpl implements AutomationService {

    private final AnimationFactory animationFactory;
    private final DeviceService deviceService;
    private final MessageService messageService;
    private final Map<Class<? extends Device>, List<Device<?>>> knownDevices = new HashMap<>();
    private Animation mainDoorOpenAnimation;

    @Setter
    @Getter
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
            closeContact.getState$()
                    // ignore the first state
                    .skip(1)
                    .subscribe(this::mainDoorStateChanged);
        }
    }

    private void mainDoorStateChanged(final CloseContactState state) {
        switch (state) {
            case OPENED -> {
                this.mainDoorOpenAnimation.play();
                log.info("MAIN-DOOR IS OPENED!");
                this.messageService.sendMessageToUser("Wohnungstür wurde geöffnet.");
            }
            case CLOSED -> {
                log.info("MAIN-DOOR IS CLOSED!");
                this.messageService.sendMessageToUser("Wohnungstür wurde geschlossen.");
            }
        }
    }
}
