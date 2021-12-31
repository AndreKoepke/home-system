package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.rest.DeconzLightResponse;
import ch.akop.homesystem.deconz.rest.DeconzSensorResponse;
import ch.akop.homesystem.deconz.rest.Specs;
import ch.akop.homesystem.deconz.rest.UpdateLightParameters;
import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.models.devices.sensor.Button;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.services.AutomationService;
import ch.akop.homesystem.services.DeviceService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;


@Service
@Slf4j
@RequiredArgsConstructor
public class DeconzConnector {

    public static final String SENSOR_PREFIX = "sensor@";
    private final Gson gson;
    private final DeviceService deviceService;
    private final DeconzConfig deconzConfig;
    private final AutomationService automationService;

    private WebClient webClient;


    @PostConstruct
    public void initialSetup() {
        this.webClient = WebClient.create("http://%s:%d/api/%s/".formatted(this.deconzConfig.getHost(),
                this.deconzConfig.getPort(),
                this.deconzConfig.getApiKey()));

        registerDevices();
        this.connect();
        this.automationService.discoverNewDevices();
    }

    @SneakyThrows
    private void connect() {
        try {
            final var wsUrl = new URI("ws://%s:%d"
                    .formatted(this.deconzConfig.getHost(), this.deconzConfig.getWebsocketPort()));

            final WebSocketClient wsClient = new WebSocketClient(wsUrl) {
                @Override
                public void onOpen(final ServerHandshake handshakedata) {
                    log.info("WebSocket is up and listing.");
                }

                @Override
                public void onMessage(final String message) {
                    try {
                        handleMessage(DeconzConnector.this.gson.fromJson(message, WebSocketUpdate.class));
                    } catch (final Exception e) {
                        log.error("Exception occurred with the message:\n{}", message, e);
                    }
                }

                @Override
                public void onClose(final int code, final String reason, final boolean remote) {
                    log.info("WS-Connection was closed, because of '{}'. Reconnecting ...", reason);
                    DeconzConnector.this.connect();
                }

                @Override
                public void onError(final Exception ex) {
                    log.error("Got exception", ex);
                }
            };

            wsClient.setConnectionLostTimeout(25);
            wsClient.connect();
        } catch (final Exception e) {
            log.error("Failed to connect. Retrying ...", e);
            Thread.sleep(1000);
            this.connect();
        }

    }


    private void registerDevices() {
        Specs.getAllSensors(this.webClient).blockOptional()
                .orElseThrow(() -> new IllegalStateException("No response from raspberry"))
                .forEach(this::registerSensor);

        Specs.getAllLights(this.webClient).blockOptional()
                .orElseThrow(() -> new IllegalStateException("Not response from raspberry"))
                .forEach(this::registerLight);

    }


    private void registerSensor(final String id, final DeconzSensorResponse sensor) {
        if (sensor.getType().contains("OpenClose")) {
            this.deviceService.registerDevice(new CloseContact()
                    .setOpen(sensor.getState().isOpen())
                    .setName(sensor.getName())
                    .setId(SENSOR_PREFIX + id)
                    .setLastChange(LocalDateTime.now()));
        }

        if (sensor.getType().equals("ZHASwitch")) {
            this.deviceService.registerDevice(new Button()
                    .setId(SENSOR_PREFIX + id)
                    .setName(sensor.getName())
                    .setLastChange(LocalDateTime.now()));
        }
    }

    private void registerLight(final String id, final DeconzLightResponse light) {
        if (light.getType().toLowerCase().contains("light")) {
            this.deviceService.registerDevice(
                    new Light((bri, duration) -> this.setBrightnessOfLight(id, bri, duration),
                            onOrOff -> {
                            })
                            .setId("light@" + id)
                            .setName(light.getName()));
        } else if (light.getType().toLowerCase().contains("on/off")) {
            this.deviceService.registerDevice(
                    new Light((bri, duration) -> this.turnOnOrOff(id, bri != 0), on -> this.turnOnOrOff(id, on))
                            .setId("light@" + id)
                            .setName(light.getName()));
        }

    }

    private void setBrightnessOfLight(final String id, final Integer bri, final Duration duration) {
        final var response = Specs.setLight(id, new UpdateLightParameters()
                                .setTransitiontime(duration != null ? (int) duration.toSeconds() * 10 : null)
                                .setBri(bri)
                                .setOn(bri > 0),
                        this.webClient)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("no response"));

        log.debug("Set light " + id + " to " + bri + " was status " + response.getStatusCode());
    }

    private void turnOnOrOff(final String id, final boolean on) {
        final var response = Specs.setLight(id, new UpdateLightParameters()
                        .setOn(on), this.webClient)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("no response"));

        log.debug("Set light " + id + " to " + (on ? "on" : "off") + " was status " + response.getStatusCode());
    }


    private void handleMessage(final WebSocketUpdate update) {
        if (update.getR().equals("sensors")
                && update.getE().equals("changed")
                && update.getState() != null) {

            if (update.getState().getOpen() != null) {
                this.deviceService.getDevice(SENSOR_PREFIX + update.getId(), CloseContact.class)
                        .setOpen(update.getState().getOpen());
            }

            if (update.getState().getButtonevent() != null) {
                this.deviceService.getDevice(SENSOR_PREFIX + update.getId(), Button.class)
                        .triggerEvent(update.getState().getButtonevent());
            }

        }
    }


}
