package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.rest.Specs;
import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.deconz.rest.models.Group;
import ch.akop.homesystem.deconz.rest.models.Light;
import ch.akop.homesystem.deconz.rest.models.Sensor;
import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.PowerMeter;
import ch.akop.homesystem.services.AutomationService;
import ch.akop.homesystem.services.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.logging.LogLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class DeconzConnector {

    public static final String NO_RESPONSE_FROM_RASPBERRY = "No response from raspberry";
    public static final String LIGHT_UPDATE_FAILED_LABEL = "Failed to update light ";
    private final DeviceService deviceService;
    private final DeconzConfig deconzConfig;
    private final AutomationService automationService;
    private final ObjectMapper objectMapper;

    private WebClient webClient;

    @PostConstruct
    public void initialSetup() {
        var strategies = ExchangeStrategies
                .builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                }).build();

        //noinspection HttpUrlsUsage
        webClient = WebClient.builder()
                .baseUrl("http://%s:%d/api/%s/".formatted(deconzConfig.getHost(),
                        deconzConfig.getPort(),
                        deconzConfig.getApiKey()))
                .exchangeStrategies(strategies)
                // full log available in DEBUG
                .clientConnector(new ReactorClientHttpConnector(reactor.netty.http.client.HttpClient.create()
                        .wiretap(getClass().getCanonicalName(), LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)))
                .build();

        registerDevices();
        automationService.discoverNewDevices();
    }

    private void registerDevices() {
        Specs.getAllSensors(webClient).blockOptional()
                .orElseThrow(() -> new IllegalStateException(NO_RESPONSE_FROM_RASPBERRY))
                .forEach(this::registerSensor);

        Specs.getAllLights(webClient).blockOptional()
                .orElseThrow(() -> new IllegalStateException(NO_RESPONSE_FROM_RASPBERRY))
                .forEach(this::registerActor);

        Specs.getAllGroups(webClient).blockOptional()
                .orElseThrow(() -> new IllegalStateException(NO_RESPONSE_FROM_RASPBERRY))
                .forEach(this::registerGroup);
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
            case "ZHASwitch" -> new Button();
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
            case "color light", "extended color light" -> Optional.of(createColorLight(id));
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

        var rollerShutter = new RollerShutter(
                lift -> Specs.setState(id, new State().setLift(lift), webClient).subscribe(),
                tilt -> Specs.setState(id, new State().setTilt(tilt), webClient).subscribe(),
                () -> Specs.setState(id, new State().setStop(true), webClient).subscribe()
        );

        return Optional.of(rollerShutter);
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
            newState.setBri((int) Math.round(percent / 100d * 255));
        }

        if (duration != null) {
            newState.setTransitiontime((int) duration.toSeconds() * 10);
        }

        Specs.setState(id, newState, webClient)
                .subscribe(
                        success -> log.debug("Set light %s to %d was status %s".formatted(id, percent, success.getStatusCode())),
                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error(LIGHT_UPDATE_FAILED_LABEL + id, throwable);
                            }
                        });
    }

    private void setColorOfLight(String id, Color color, Duration duration) {
        var newState = new State()
                .setXy(color.toXY())
                .setColormode("ct");

        if (duration != null) {
            newState.setTransitiontime((int) duration.toSeconds() * 10);
        }

        Specs.setState(id, newState, webClient)
                .subscribe(
                        success -> log.debug("Set color of light %s was status %s".formatted(id, success.getStatusCode())),
                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error(LIGHT_UPDATE_FAILED_LABEL + id, throwable);
                            }
                        });
    }

    private void turnOnOrOff(String id, boolean on) {
        Specs.setState(id, new State().setOn(on), webClient)
                .subscribe(
                        response -> log.debug("Set light {} to {} was status {}", id, on ? "on" : "off", response.getStatusCode()),
                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error(LIGHT_UPDATE_FAILED_LABEL + id, throwable);
                            }
                        }
                );
    }

    private void activateScene(String sceneId, String groupId) {
        Specs.activateScene(groupId, sceneId, webClient)
                .subscribe(
                        response -> log.debug("Scene {} in group {} activated", sceneId, groupId),
                        throwable -> log.error("Failed to set scene {} in group {}", sceneId, groupId, throwable)
                );
    }


    public void handleMessage(WebSocketUpdate update) {
        if (update.getR().equals("sensors")
                && update.getE().equals("changed")
                && update.getState() != null) {

            deviceService.findDeviceById(update.getId(), Device.class)
                    .ifPresent(device -> device.consumeUpdate(update.getState()));
        } else if (update.getR().equals("lights")
                && update.getE().equals("changed")
                && update.getState() != null
                && update.getState().getOn() != null) {

            deviceService.findDeviceById(update.getId(), Device.class)
                    .ifPresent(device -> device.consumeUpdate(update.getState()));
        }
    }
}
