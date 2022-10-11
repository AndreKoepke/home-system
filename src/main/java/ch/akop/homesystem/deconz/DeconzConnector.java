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
import java.time.LocalDateTime;
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
            log.warn("Sensortype '{}' isn't known", sensor.getType());
            return;
        }

        newDevice.setId(id);
        newDevice.setName(sensor.getName());
        newDevice.setLastChange(LocalDateTime.now());

        deviceService.registerDevice(newDevice);

    }

    @Nullable
    private Device<?> determineSensor(Sensor sensor) {
        return switch (sensor.getType()) {

            case "ZHAOpenClose" -> new CloseContact()
                    .setOpen(sensor.getState().getOpen());

            case "ZHASwitch" -> new Button();

            case "ZHAPresence" -> {
                var motionSensor = new MotionSensor();
                motionSensor.updateState(sensor.getState().getPresence(), sensor.getState().getDark());
                yield motionSensor;
            }

            case "ZHAPower" -> {
                var powerMeter = new PowerMeter();
                powerMeter.getPower$().onNext(sensor.getState().getPower());
                powerMeter.getCurrent$().onNext(sensor.getState().getCurrent());
                powerMeter.getVoltage$().onNext(sensor.getState().getVoltage());
                yield powerMeter;
            }

            default -> null;
        };
    }

    private void registerActor(String id, Light light) {
        tryCreateLight(id, light)
                .or(() -> tryCreateRollerShutter(id, light))
                .map(newLight -> newLight.setId(id))
                .map(newLight -> newLight.setName(light.getName()))
                .ifPresentOrElse(
                        deviceService::registerDevice,
                        () -> log.warn("Device-type '{}' isn't implemented", light.getType()));
    }

    private Optional<Device<?>> tryCreateLight(String id, Light light) {
        return switch (light.getType().toLowerCase()) {
            case "color light", "extended color light" -> Optional.of(createColorLight(id, light));
            case "dimmable light", "color temperature light" -> Optional.of(createDimmableLight(id, light));
            case "on/off plug-in unit", "on/off light" -> Optional.of(registerSimpleLight(id, light));
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

        // workaround - lift is reported via bri, for tilt there are no values
        rollerShutter.setCurrentLift(light.getState().getBri());

        return Optional.of(rollerShutter);
    }

    private ColoredLight createColorLight(String id, Light light) {
        var coloredLight = new ColoredLight(
                (percent, duration) -> setBrightnessOfLight(id, percent, duration),
                turnOn -> turnOnOrOff(id, turnOn),
                (color, duration) -> setColorOfLight(id, color, duration)
        );

        coloredLight.updateState(light.getState().getOn());
        return coloredLight;
    }

    private DimmableLight createDimmableLight(String id, Light light) {
        var dimmableLight = new DimmableLight(
                (percent, duration) -> setBrightnessOfLight(id, percent, duration),
                on -> turnOnOrOff(id, on));
        dimmableLight.updateState(light.getState().getOn());
        return dimmableLight;
    }

    private SimpleLight registerSimpleLight(String id, Light light) {
        return new SimpleLight(on -> turnOnOrOff(id, on))
                .updateState(light.getState().getOn());
    }

    private void setBrightnessOfLight(String id, Integer percent, Duration duration) {
        Specs.setState(
                        id,
                        new State()
                                .setTransitiontime(duration != null ? (int) duration.toSeconds() * 10 : null)
                                .setBri((int) Math.round(percent / 100d * 255))
                                .setOn(percent > 0),
                        webClient)
                .subscribe(
                        success -> log.debug("Set light %s to %d was status %s".formatted(id, percent, success.getStatusCode())),
                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error(LIGHT_UPDATE_FAILED_LABEL + id, throwable);
                            }
                        });
    }

    private void setColorOfLight(String id, Color color, Duration duration) {
        Specs.setState(
                        id,
                        new State()
                                .setTransitiontime(duration != null ? (int) duration.toSeconds() * 10 : null)
                                .setXy(color.toXY())
                                .setColormode("ct"),
                        webClient)
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
                        response -> log.debug("Set light %s to %s was status %s"
                                .formatted(id, on ? "on" : "off", response.getStatusCode())),

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
                        response -> log.debug("Scene %s in group %s activated".formatted(sceneId, groupId)),
                        throwable -> log.error("Failed to set scene {} in group {}", sceneId, groupId, throwable)
                );
    }


    public void handleMessage(WebSocketUpdate update) {
        if (update.getR().equals("sensors")
                && update.getE().equals("changed")
                && update.getState() != null) {

            updateSensor(update.getId(), update.getState());
        }

        if (update.getR().equals("lights")
                && update.getE().equals("changed")
                && update.getState() != null
                && update.getState().getOn() != null) {

            updateActor(update.getId(), update.getState());
        }

    }

    private void updateActor(String actorId, State state) {
        if (state.getTilt() != null || state.getLift() != null) {
            var rollerShutter = deviceService.getDeviceById(actorId, RollerShutter.class);
            // bri as workaround, tilt was never updated
            rollerShutter.setCurrentLift(state.getBri());
        } else {
            deviceService.getDeviceById(actorId, SimpleLight.class).updateState(state.getOn());
        }
    }

    private void updateSensor(String sensorId, State state) {
        if (state.getOpen() != null) {
            deviceService.getDeviceById(sensorId, CloseContact.class).setOpen(state.getOpen());
        } else if (state.getButtonevent() != null) {
            deviceService.getDeviceById(sensorId, Button.class).triggerEvent(state.getButtonevent());
        } else if (state.getPresence() != null) {
            deviceService.getDeviceById(sensorId, MotionSensor.class).updateState(state.getPresence(), state.getDark());
        } else if (state.getPower() != null) {
            var powerMeter = deviceService.getDeviceById(sensorId, PowerMeter.class);
            powerMeter.getPower$().onNext(state.getPower());
            powerMeter.getCurrent$().onNext(state.getCurrent());
            powerMeter.getVoltage$().onNext(state.getVoltage());
        }
    }
}
