package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.rest.Specs;
import ch.akop.homesystem.deconz.rest.UpdateLightParameters;
import ch.akop.homesystem.deconz.rest.models.Group;
import ch.akop.homesystem.deconz.rest.models.Light;
import ch.akop.homesystem.deconz.rest.models.Sensor;
import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.services.AutomationService;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.UserService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
                final var retryIn = Duration.of(Math.max(60, DeconzConnector.this.connectionRetries++ * 10), SECONDS);
                log.warn("WS-Connection was closed, because of '{}'. Reconnecting  in {}s...", reason, retryIn.toSeconds());
                Thread.sleep(retryIn.toMillis());
                DeconzConnector.this.connect();
            }

            @Override
            public void onError(final Exception ex) {
                if (DeconzConnector.this.connectionRetries == 0) {
                    DeconzConnector.this.userService.devMessage("Lost connection to raspberry. :(");
                    log.error("Got exception", ex);
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
                .forEach(this::registerLight);

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
        if (sensor.getType().contains("OpenClose")) {
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
    }

    private void registerLight(final String id, final Light light) {
        if (light.getType().toLowerCase().contains("light")) {
            this.deviceService.registerDevice(
                    new ch.akop.homesystem.models.devices.actor.Light(
                            (bri, duration) -> this.setBrightnessOfLight(id, bri, duration), onOrOff -> {})
                            .setId(id)
                            .setName(light.getName())
                            .setOn(light.getState().isOn()));

        } else if (light.getType().toLowerCase().contains("on/off")) {
            this.deviceService.registerDevice(
                    new ch.akop.homesystem.models.devices.actor.Light(
                            (bri, duration) -> this.turnOnOrOff(id, bri != 0), on -> this.turnOnOrOff(id, on))
                            .setId(id)
                            .setName(light.getName())
                            .setOn(light.getState().isOn()));
        }

    }

    private void setBrightnessOfLight(final String id, final Integer bri, final Duration duration) {
        Specs.setLight(
                        id,
                        new UpdateLightParameters()
                                .setTransitiontime(duration != null ? (int) duration.toSeconds() * 10 : null)
                                .setBri(bri)
                                .setOn(bri > 0),
                        this.webClient)
                .subscribe(
                        success -> log.debug("Set light %s to %d was status %s".formatted(id, bri, success.getStatusCode())),
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
        }

        if (update.getR().equals("lights")
                && update.getE().equals("changed")
                && update.getState() != null
                && update.getState().getOn() != null) {

            this.deviceService.getDevice(update.getId(), ch.akop.homesystem.models.devices.actor.Light.class)
                    .setOn(update.getState().getOn());
        }

    }


}
