package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.util.EventConstants.BUTTON;
import static ch.akop.homesystem.util.EventConstants.BUTTON_INTERNAL;
import static ch.akop.homesystem.util.EventConstants.CLOSE_CONTACT;
import static ch.akop.homesystem.util.EventConstants.CUBE;
import static ch.akop.homesystem.util.EventConstants.GENERAL;

import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.sensor.AqaraCube;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.models.devices.sensor.CloseContactState;
import ch.akop.homesystem.models.events.ButtonPressEvent;
import ch.akop.homesystem.models.events.ButtonPressInternalEvent;
import ch.akop.homesystem.models.events.CloseContactEvent;
import ch.akop.homesystem.models.events.CubeEvent;
import ch.akop.homesystem.models.events.CubeEventType;
import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.persistence.repository.config.OffButtonConfigRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Dependent
@Priority(500)
public class AutomationService {

  private static final int MARCEL_CONSTANT_SECONDS = 30;

  private final DeviceService deviceService;
  private final BasicConfigRepository basicConfigRepository;
  private final OffButtonConfigRepository offButtonConfigRepository;
  private final PowerMeterService powerMeterService;
  private final MotionSensorService motionSensorService;
  private final RollerShutterService rollerShutterService;
  private final LightnessControlledDeviceService lightnessControlledDeviceService;
  private final EventBus eventPublisher;

  @SuppressWarnings("rawtypes")
  private final Map<Class<? extends Device>, List<Device<?>>> knownDevices = new HashMap<>();


  @SneakyThrows
  public void discoverNewDevices() {
    deviceService.getAllDevices().stream()
        .filter(this::unknownDevice)
        .forEach(this::addDevice);

    powerMeterService.init();
    motionSensorService.init();
    rollerShutterService.init();
    lightnessControlledDeviceService.init();
  }

  private boolean unknownDevice(Device<?> device) {
    return !knownDevices.computeIfAbsent(device.getClass(), aClass -> new ArrayList<>())
        .contains(device);
  }

  private void addDevice(Device<?> device) {
    knownDevices.get(device.getClass())
        .add(device);

    if (device instanceof CloseContact closeContact) {
      //noinspection ResultOfMethodCallIgnored
      basicConfigRepository.findByOrderByModifiedDesc()
          .map(BasicConfig::getMainDoorName)
          .filter(mainDoorName -> mainDoorName.equals(closeContact.getName()))
          .ifPresent(mainDoorName -> closeContact.getState$()
              .skip(1)
              .distinctUntilChanged()
              .throttleLatest(MARCEL_CONSTANT_SECONDS, TimeUnit.SECONDS)
              .subscribe(this::mainDoorStateChanged));

      //noinspection ResultOfMethodCallIgnored
      closeContact.getState$()
          .skip(1)
          .distinctUntilChanged()
          .subscribe(newState -> eventPublisher.publish(CLOSE_CONTACT, new CloseContactEvent(closeContact.getName(), newState)));
    }

    if (device instanceof Button button) {
      //noinspection ResultOfMethodCallIgnored
      button.getEvents$().subscribe(integer -> eventPublisher.publish(BUTTON_INTERNAL,
          new ButtonPressInternalEvent(button.getName(), integer)));
    } else if (device instanceof AqaraCube cube) {
      //noinspection ResultOfMethodCallIgnored
      cube.getActiveSide$()
          .skip(1)
          .subscribe(activeSide -> eventPublisher.publish(CUBE, new CubeEvent(cube.getName(), determineFlippedSide(activeSide))));
      //noinspection ResultOfMethodCallIgnored
      cube.getShacked$()
          .subscribe(empty -> eventPublisher.publish(CUBE, new CubeEvent(cube.getName(), CubeEventType.SHAKED)));
    }
  }

  private CubeEventType determineFlippedSide(int side) {
    return switch (side) {
      case 1 -> CubeEventType.FLIPPED_TO_SIDE_1;
      case 2 -> CubeEventType.FLIPPED_TO_SIDE_2;
      case 3 -> CubeEventType.FLIPPED_TO_SIDE_3;
      case 4 -> CubeEventType.FLIPPED_TO_SIDE_4;
      case 5 -> CubeEventType.FLIPPED_TO_SIDE_5;
      case 6 -> CubeEventType.FLIPPED_TO_SIDE_6;
      default -> throw new IllegalArgumentException("Cube-Side %d not existing".formatted(side));
    };
  }

  @ConsumeEvent(value = BUTTON_INTERNAL, blocking = true)
  public void buttonWasPressed(ButtonPressInternalEvent internalEvent) {
    if (wasCentralOffPressed(internalEvent.getButtonName(), internalEvent.getButtonEvent())) {
      eventPublisher.publish(GENERAL, Event.CENTRAL_OFF_PRESSED);
    } else if (wasGoodNightButtonPressed(internalEvent.getButtonName(), internalEvent.getButtonEvent())) {
      eventPublisher.publish(GENERAL, Event.GOOD_NIGHT_PRESSED);
    } else {
      eventPublisher.publish(BUTTON, new ButtonPressEvent(internalEvent.getButtonName(), internalEvent.getButtonEvent()));
    }
  }


  private boolean wasGoodNightButtonPressed(String buttonName, int buttonEvent) {
    var basicConfig = basicConfigRepository.findByOrderByModifiedDesc().orElseThrow();
    if (basicConfig.getGoodNightButtonName() == null || basicConfig.getGoodNightButtonEvent() == null) {
      return false;
    }

    return basicConfig.getGoodNightButtonName().equals(buttonName)
        && basicConfig.getGoodNightButtonEvent().equals(buttonEvent);
  }

  private boolean wasCentralOffPressed(String buttonName, int buttonEvent) {
    return offButtonConfigRepository.findAllByNameAndButtonEvent(buttonName, buttonEvent)
        .findAny().isPresent();
  }


  private void mainDoorStateChanged(CloseContactState state) {
    if (state == CloseContactState.CLOSED) {
      eventPublisher.publish(GENERAL, Event.DOOR_CLOSED);
    } else {
      eventPublisher.publish(GENERAL, Event.DOOR_OPENED);
    }
  }
}
