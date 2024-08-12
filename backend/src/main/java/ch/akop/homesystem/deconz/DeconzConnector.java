package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.rest.DeconzService;
import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.deconz.rest.models.Group;
import ch.akop.homesystem.deconz.rest.models.Light;
import ch.akop.homesystem.deconz.rest.models.Sensor;
import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.Actor;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.models.devices.sensor.AqaraCube;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.PowerMeter;
import ch.akop.homesystem.persistence.model.config.DeconzConfig;
import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import ch.akop.homesystem.persistence.repository.config.DeconzConfigRepository;
import ch.akop.homesystem.persistence.repository.config.RollerShutterConfigRepository;
import ch.akop.homesystem.services.impl.AutomationService;
import ch.akop.homesystem.services.impl.DeviceService;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.springframework.lang.Nullable;


@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
@Startup
public class DeconzConnector {

  private final DeviceService deviceService;
  private final AutomationService automationService;
  private final DeconzConfigRepository deconzConfigRepository;
  private final RollerShutterConfigRepository rollerShutterConfigRepository;

  @Getter
  private AtomicBoolean isConnected = new AtomicBoolean(false);

  DeconzService deconzService;


  @Transactional
  void tryToStart(@Observes StartupEvent event) {
    // TODO restart when config changes
    var config = deconzConfigRepository.getFirstByOrderByModifiedDesc();

    if (config == null) {
      log.warn("No deCONZ found. DeCONZ-Service will not be started.");
      return;
    }

    try {
      connectToDeconz(config);
    } catch (Exception e) {
      log.error("Cannot connect to deconz", e);
    }
  }

  @SneakyThrows
  private void connectToDeconz(DeconzConfig config) {
    deconzService = RestClientBuilder.newBuilder()
        .baseUrl(new URL("http://%s:%d/api/%s/".formatted(config.getHost(),
            config.getPort(),
            config.getApiKey())))
        .build(DeconzService.class);

    registerDevices();
    automationService.discoverNewDevices();

    log.info("deCONZ is up");
  }

  private void registerDevices() {
    deconzService.getAllSensors().forEach(this::registerSensor);
    deconzService.getAllLights().forEach(this::registerActor);
    deconzService.getAllGroups().forEach(this::registerGroup);
  }

  private void registerGroup(String id, Group group) {
    var newGroup = new ch.akop.homesystem.models.devices.other.Group()
        .setName(group.getName())
        .setLights(group.getLights())
        .setId(id);

    newGroup.setScenes(group.getScenes().stream()
        .map(scene -> new Scene(newGroup, () -> activateScene(scene.getId(), group.getId()))
            .setId(scene.getId())
            .setName(scene.getName()))
        .toList());

    deviceService.registerDevice(newGroup);
  }

  private void registerSensor(String id, Sensor sensor) {

    var newDevice = determineSensor(sensor);

    if (newDevice == null) {
      log.warn("Sensor of type '{}' isn't known", sensor.getType());
      return;
    }

    newDevice.setId(id);
    newDevice.setName(sensor.getName());
    newDevice.consumeUpdate(sensor.getState());

    deviceService.registerDevice(newDevice);
  }

  @Nullable
  private Device<?> determineSensor(Sensor sensor) {
    return switch (sensor.getType()) {
      case "ZHAOpenClose" -> new CloseContact();
      case "ZHASwitch" -> sensor.getModelid().equals("lumi.sensor_cube.aqgl01") ? new AqaraCube() : new Button();
      case "ZHAPresence" -> new MotionSensor();
      case "ZHAPower" -> new PowerMeter();
      default -> null;
    };
  }

  private void registerActor(String id, Light light) {
    tryCreateLight(id, light)
        .or(() -> tryCreateRollerShutter(id, light))
        .map(newLight -> newLight.setId(id))
        .map(newLight -> newLight.setName(light.getName()))
        .map(newLight -> newLight.consumeUpdate(light.getState()))
        .ifPresentOrElse(
            deviceService::registerDevice,
            () -> log.warn("Device of type '{}' isn't implemented", light.getType()));
  }

  private Optional<Device<?>> tryCreateLight(String id, Light light) {
    return switch (light.getType().toLowerCase()) {
      case "color light", "extended color light", "color dimmable light" -> Optional.of(createColorLight(id));
      case "dimmable light", "color temperature light" -> Optional.of(createDimmableLight(id));
      case "on/off plug-in unit", "on/off light" -> Optional.of(registerSimpleLight(id));
      default -> Optional.empty();
    };
  }

  private Optional<RollerShutter> tryCreateRollerShutter(String id, Light light) {
    if (!light.getType().equalsIgnoreCase("Window covering controller")
        && !light.getType().equalsIgnoreCase("Window covering device")) {
      return Optional.empty();
    }

    Consumer<Integer> tiltFunction;
    if (light.getState().getTilt() != null) {
      tiltFunction = tilt -> updateLight(id, new State().setTilt(tilt));
    } else {
      log.info("RollerShutter {} has no tilt-function", light.getName());
      tiltFunction = tilt -> {
      };
    }

    var rollerShutter = new RollerShutter(
        lift -> updateLight(id, new State().setLift(lift)),
        tiltFunction,
        light.getState().getTilt() != null,
        rollerShutterConfigRepository.findByNameLike(light.getName())
            .map(RollerShutterConfig::getCloseWithInterrupt)
            .orElse(false)
    );

    return Optional.of(rollerShutter);
  }

  private void updateLight(String id, State newState) {
    if (!isConnected.get()) {
      log.warn("Not connected. Ignored update for {} with {}", id, newState);
      return;
    }

    deconzService.updateLight(id, newState);
  }

  private ColoredLight createColorLight(String id) {
    return new ColoredLight(
        (percent, duration) -> setBrightnessOfLight(id, percent, duration),
        turnOn -> turnOnOrOff(id, turnOn),
        (color, duration) -> setColorOfLight(id, color, duration)
    );
  }

  private DimmableLight createDimmableLight(String id) {
    return new DimmableLight(
        (percent, duration) -> setBrightnessOfLight(id, percent, duration),
        on -> turnOnOrOff(id, on));
  }

  private SimpleLight registerSimpleLight(String id) {
    return new SimpleLight(on -> turnOnOrOff(id, on));
  }

  private void setBrightnessOfLight(String id, Integer percent, Duration duration) {
    var newState = new State();

    if (percent == 0) {
      newState.setOn(false);
    } else {
      newState.setBri((int) Math.round(percent / 100d * 255))
          .setOn(true);
    }

    if (duration != null) {
      newState.setTransitiontime((int) duration.toSeconds() * 10);
    }

    updateLight(id, newState);
  }

  private void setColorOfLight(String id, Color color, Duration duration) {
    var newState = new State()
        .setXy(color.toXY())
        // always set on, otherwise the color-command will be discarded
        .setOn(true);

    if (duration != null) {
      newState.setTransitiontime((int) duration.toSeconds() * 10);
    }

    updateLight(id, newState);
  }


  private void turnOnOrOff(String id, boolean on) {
    updateLight(id, new State().setOn(on));
  }

  private void activateScene(String sceneId, String groupId) {
    deconzService.activateScene(groupId, sceneId);
  }


  public void handleMessage(WebSocketUpdate update) {
    if (update.getR().equals("sensors")
        && update.getE().equals("changed")
        && update.getState() != null) {

      deviceService.findDeviceById(update.getId(), ch.akop.homesystem.models.devices.sensor.Sensor.class)
          .ifPresent(device -> device.consumeUpdate(update.getState()));
    } else if (update.getR().equals("lights")
        && update.getE().equals("changed")
        && update.getState() != null) {

      deviceService.findDeviceById(update.getId(), Actor.class)
          .ifPresent(device -> device.consumeUpdate(update.getState()));
    }
  }
}
