package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.rest.Specs;
import ch.akop.homesystem.deconz.rest.UpdateLightParameters;
import ch.akop.homesystem.deconz.rest.models.Group;
import ch.akop.homesystem.deconz.rest.models.Light;
import ch.akop.homesystem.deconz.rest.models.Sensor;
import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.services.AutomationService;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.UserService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.NotYetImplementedException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.SECONDS;


@Service
@Slf4j
@RequiredArgsConstructor
public class DeconzConnector {

    public static final String NO_RESPONSE_FROM_RASPBERRY = "No response from raspberry";
    public static final String DEVICE_TYPE_CLOSE_CONTACT = "OpenClose";
    public static final String DEVICE_TYPE_MOTION_SENSOR = "ZHAPresence";
    public static final String LIGHT_UPDATE_FAILED_LABEL = "Failed to update light ";
    private final Gson gson;
    private final DeviceService deviceService;
    private final DeconzConfig deconzConfig;
    private final AutomationService automationService;
    private final UserService userService;

    private WebClient webClient;
    private int connectionRetries = 0;


    @PostConstruct
    public void initialSetup() {
        //noinspection HttpUrlsUsage
        this.webClient = WebClient.builder()
                .baseUrl("http://%s:%d/api/%s/".formatted(this.deconzConfig.getHost(),
                        this.deconzConfig.getPort(),
                        this.deconzConfig.getApiKey()))
                .build();

        registerDevices();
        this.connect();
        this.automationService.discoverNewDevices();

        this.deviceService.getDevicesOfType(ColoredLight.class)
                .forEach(light -> light.setColor(Color.BLUE(), Duration.ofSeconds(1)));
    }

    @SneakyThrows
    private void connect() {
        var wsUrl = new URI("ws://%s:%d"
                .formatted(this.deconzConfig.getHost(), this.deconzConfig.getWebsocketPort()));

        WebSocketClient wsClient = new WebSocketClient(wsUrl, new Draft_6455(), null, 30) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                log.info("WebSocket is up and listing.");
                DeconzConnector.this.connectionRetries = 0;
            }

            @Override
            public void onMessage(String message) {
                try {
                    handleMessage(DeconzConnector.this.gson.fromJson(message, WebSocketUpdate.class));
                } catch (Exception e) {
                    log.error("Exception occurred with the message:\n{}", message, e);
                }
            }

            @SneakyThrows
            @Override
            public void onClose(int code, String reason, boolean remote) {
                var retryIn = Duration.of(Math.min(60, ++DeconzConnector.this.connectionRetries * 10), SECONDS);
                log.warn("WS-Connection was closed, because of '{}'. Reconnecting  in {}s...", reason, retryIn.toSeconds());
                Thread.sleep(retryIn.toMillis());
                DeconzConnector.this.connect();
            }

            @Override
            public void onError(Exception ex) {
                if (DeconzConnector.this.connectionRetries <= 1) {
                    DeconzConnector.this.userService.devMessage("Lost connection to raspberry. :(");
                    log.error("Got exception on ws-connection to {}", DeconzConnector.this.deconzConfig.getHost(), ex);
                }
            }
        };

        wsClient.setConnectionLostTimeout(25);
        wsClient.connect();
    }


    private void registerDevices() {
        Specs.getAllSensors(this.webClient).blockOptional()
                .orElseThrow(() -> new IllegalStateException(NO_RESPONSE_FROM_RASPBERRY))
                .forEach(this::registerSensor);

        Specs.getAllLights(this.webClient).blockOptional()
                .orElseThrow(() -> new IllegalStateException(NO_RESPONSE_FROM_RASPBERRY))
                .forEach(this::registerActor);

        Specs.getAllGroups(this.webClient).blockOptional()
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

        this.deviceService.registerDevice(newGroup);
    }

    private void registerSensor(String id, Sensor sensor) {
        if (sensor.getType().contains(DEVICE_TYPE_CLOSE_CONTACT)) {
            this.deviceService.registerDevice(new CloseContact()
                    .setOpen(sensor.getState().isOpen())
                    .setName(sensor.getName())
                    .setId(id)
                    .setLastChange(LocalDateTime.now()));
        }

        if (sensor.getType().equals("ZHASwitch")) {
            this.deviceService.registerDevice(new Button()
                    .setId(id)
                    .setName(sensor.getName())
                    .setLastChange(LocalDateTime.now()));
        }

        if (sensor.getType().equals(DEVICE_TYPE_MOTION_SENSOR)) {
            this.deviceService.registerDevice(new MotionSensor()
                    .setId(id)
                    .setName(sensor.getName())
                    .setLastChange(LocalDateTime.now()));
        }
    }

    private void registerActor(String id, Light light) {
        getLightInstanceByType(id, light)
                .map(newLight -> newLight.setId(id))
                .map(newLight -> newLight.setName(light.getName()))
                .ifPresent(this.deviceService::registerDevice);
    }

    private Optional<SimpleLight> getLightInstanceByType(String id, Light light) {
        return switch (light.getType().toLowerCase()) {
            case "color light", "extended color light" -> Optional.of(registerColorLight(id, light));
            case "dimmable light", "color temperature light" -> Optional.of(registerDimmableLight(id, light));
            case "on/off plug-in unit", "on/off light" -> Optional.of(registerSimpleLight(id, light));
            case "configuration tool" -> Optional.empty();
            default ->
                    throw new NotYetImplementedException("Actor of type %s is not implemented".formatted(light.getType()));
        };
    }

    private ColoredLight registerColorLight(String id, Light light) {
        var coloredLight = new ColoredLight(
                (percent, duration) -> this.setBrightnessOfLight(id, percent, duration),
                turnOn -> this.turnOnOrOff(id, turnOn),
                (color, duration) -> this.setColorOfLight(id, color, duration)
        );

        coloredLight.updateState(light.getState().isOn());
        return coloredLight;
    }

    private DimmableLight registerDimmableLight(String id, Light light) {
        var dimmableLight = new DimmableLight(
                (percent, duration) -> this.setBrightnessOfLight(id, percent, duration),
                on -> this.turnOnOrOff(id, on));
        dimmableLight.updateState(light.getState().isOn());
        return dimmableLight;
    }

    private SimpleLight registerSimpleLight(String id, Light light) {
        return new SimpleLight(on -> this.turnOnOrOff(id, on))
                .updateState(light.getState().isOn());
    }

    private void setBrightnessOfLight(String id, Integer percent, Duration duration) {
        Specs.setLight(
                        id,
                        new UpdateLightParameters()
                                .setTransitiontime(duration != null ? (int) duration.toSeconds() * 10 : null)
                                .setBri((int) Math.round(percent / 100d * 255))
                                .setOn(percent > 0),
                        this.webClient)
                .subscribe(
                        success -> log.debug("Set light %s to %d was status %s".formatted(id, percent, success.getStatusCode())),
                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error(LIGHT_UPDATE_FAILED_LABEL + id, throwable);
                            }
                        });
    }

    private void setColorOfLight(String id, Color color, Duration duration) {
        Specs.setLight(
                        id,
                        new UpdateLightParameters()
                                .setTransitiontime(duration != null ? (int) duration.toSeconds() * 10 : null)
                                .setXy(color.toXY())
                                .setColormode("ct"),
                        this.webClient)
                .subscribe(
                        success -> log.debug("Set color of light %s was status %s".formatted(id, success.getStatusCode())),
                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error(LIGHT_UPDATE_FAILED_LABEL + id, throwable);
                            }
                        });
    }

    private void turnOnOrOff(String id, boolean on) {
        Specs.setLight(id, new UpdateLightParameters().setOn(on), this.webClient)
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
        Specs.activateScene(groupId, sceneId, this.webClient)
                .subscribe(
                        response -> log.debug("Scene %s in group %s activated".formatted(sceneId, groupId)),
                        throwable -> log.error("Failed to set scene {} in group {}", sceneId, groupId, throwable)
                );
    }


    private void handleMessage(WebSocketUpdate update) {
        if (update.getR().equals("sensors")
                && update.getE().equals("changed")
                && update.getState() != null) {

            if (update.getState().getOpen() != null) {
                this.deviceService.getDeviceById(update.getId(), CloseContact.class)
                        .setOpen(update.getState().getOpen());
            }

            if (update.getState().getButtonevent() != null) {
                this.deviceService.getDeviceById(update.getId(), Button.class)
                        .triggerEvent(update.getState().getButtonevent());
            }

            if (update.getState().getPresence() != null) {
                this.deviceService.getDeviceById(update.getId(), MotionSensor.class)
                        .updateState(update.getState().getPresence(), update.getState().getDark());
            }
        }

        if (update.getR().equals("lights")
                && update.getE().equals("changed")
                && update.getState() != null
                && update.getState().getOn() != null) {

            this.deviceService.getDeviceById(update.getId(), SimpleLight.class)
                    .updateState(update.getState().getOn());
        }

    }


}
