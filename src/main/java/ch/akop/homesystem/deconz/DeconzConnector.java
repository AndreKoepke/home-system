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

import static java.time.temporal.ChronoUnit.SECONDS;


@Service
@Slf4j
@RequiredArgsConstructor
public class DeconzConnector {

    public static final String NO_RESPONSE_FROM_RASPBERRY = "No response from raspberry";
    public static final String DEVICE_TYPE_CLOSE_CONTACT = "OpenClose";
    public static final String DEVICE_TYPE_MOTION_SENSOR = "ZHAPresence";
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
    }

    @SneakyThrows
    private void connect() {
        final var wsUrl = new URI("ws://%s:%d"
                .formatted(this.deconzConfig.getHost(), this.deconzConfig.getWebsocketPort()));

        final WebSocketClient wsClient = new WebSocketClient(wsUrl, new Draft_6455(), null, 30) {
            @Override
            public void onOpen(final ServerHandshake handshakedata) {
                log.info("WebSocket is up and listing.");
                DeconzConnector.this.connectionRetries = 0;
            }

            @Override
            public void onMessage(final String message) {
                try {
                    handleMessage(DeconzConnector.this.gson.fromJson(message, WebSocketUpdate.class));
                } catch (final Exception e) {
                    log.error("Exception occurred with the message:\n{}", message, e);
                }
            }

            @SneakyThrows
            @Override
            public void onClose(final int code, final String reason, final boolean remote) {
                final var retryIn = Duration.of(Math.min(60, ++DeconzConnector.this.connectionRetries * 10), SECONDS);
                log.warn("WS-Connection was closed, because of '{}'. Reconnecting  in {}s...", reason, retryIn.toSeconds());
                Thread.sleep(retryIn.toMillis());
                DeconzConnector.this.connect();
            }

            @Override
            public void onError(final Exception ex) {
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

    private void registerGroup(final String id, final Group group) {
        final var newGroup = new ch.akop.homesystem.models.devices.other.Group()
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

    private void registerSensor(final String id, final Sensor sensor) {
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

    private void registerActor(final String id, final Light light) {
        var actor = getLightInstanceByType(id, light);
        actor.setName(light.getName());
        actor.setId(id);

        deviceService.registerDevice(actor);
    }

    private SimpleLight getLightInstanceByType(String id, Light light) {
        return switch (light.getType().toLowerCase()) {
            case "color temperature light", "color light", "extended color light" -> registerColorLight(id, light);
            case "dimmable light" -> registerDimmableLight(id, light);
            case "on/off plug-in unit" -> registerSimpleLight(id, light);
            default -> throw new NotYetImplementedException("Actor of type %s is not implemented".formatted(light.getType()));
        };
    }

    private ColoredLight registerColorLight(String id, Light light) {
        var coloredLight = new ColoredLight(
                (percent, duration) -> this.setBrightnessOfLight(id, percent, duration),
                turnOn -> this.turnOnOrOff(id, turnOn),
                (percent, color, duration) -> this.setColorOfLight(id, percent, color, duration)
        );

        coloredLight.setOn(light.getState().isOn());
        return coloredLight;
    }

    private DimmableLight registerDimmableLight(String id, Light light) {
        var dimmableLight = new DimmableLight(
                (percent, duration) -> this.setBrightnessOfLight(id, percent, duration),
                on -> this.turnOnOrOff(id, on));
        dimmableLight.setOn(light.getState().isOn());
        return dimmableLight;
    }

    private SimpleLight registerSimpleLight(String id, Light light) {
        return new SimpleLight(on -> this.turnOnOrOff(id, on))
                .setOn(light.getState().isOn());
    }

    private void setBrightnessOfLight(final String id, final Integer percent, final Duration duration) {
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
                                log.error("Failed to update light " + id, throwable);
                            }
                        });
    }

    private void setColorOfLight(final String id, final Integer percent, Color color, final Duration duration) {
        Specs.setLight(
                        id,
                        new UpdateLightParameters()
                                .setTransitiontime(duration != null ? (int) duration.toSeconds() * 10 : null)
                                .setBri((int) Math.round(percent / 100d * 255))
                                .setXy(color.toXY())
                                .setOn(percent > 0),
                        this.webClient)
                .subscribe(
                        success -> log.debug("Set light %s to %d was status %s".formatted(id, percent, success.getStatusCode())),
                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error("Failed to update light " + id, throwable);
                            }
                        });
    }

    private void turnOnOrOff(final String id, final boolean on) {
        Specs.setLight(id, new UpdateLightParameters().setOn(on), this.webClient)
                .subscribe(
                        response -> log.debug("Set light %s to %s was status %s"
                                .formatted(id, on ? "on" : "off", response.getStatusCode())),

                        throwable -> {
                            if (!throwable.getClass().equals(InterruptedException.class)) {
                                log.error("Failed to update light " + id, throwable);
                            }
                        }
                );
    }

    private void activateScene(final String sceneId, final String groupId) {
        Specs.activateScene(groupId, sceneId, this.webClient)
                .subscribe(
                        response -> log.debug("Scene %s in group %s activated".formatted(sceneId, groupId)),
                        throwable -> log.error("Failed to set scene {} in group {}", sceneId, groupId, throwable)
                );
    }


    private void handleMessage(final WebSocketUpdate update) {
        if (update.getR().equals("sensors")
                && update.getE().equals("changed")
                && update.getState() != null) {

            if (update.getState().getOpen() != null) {
                this.deviceService.getDevice(update.getId(), CloseContact.class)
                        .setOpen(update.getState().getOpen());
            }

            if (update.getState().getButtonevent() != null) {
                this.deviceService.getDevice(update.getId(), Button.class)
                        .triggerEvent(update.getState().getButtonevent());
            }

            if (update.getState().getPresence() != null) {
                this.deviceService.getDevice(update.getId(), MotionSensor.class)
                        .updateState(update.getState().getPresence(), update.getState().getDark());
            }
        }

        if (update.getR().equals("lights")
                && update.getE().equals("changed")
                && update.getState() != null
                && update.getState().getOn() != null) {

            this.deviceService.getDevice(update.getId(), SimpleLight.class)
                    .setOn(update.getState().getOn());
        }

    }


}
