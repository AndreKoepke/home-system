package ch.akop.homesystem.models.deconz;

import ch.akop.homesystem.models.deconz.rest.DeconzLightResponse;
import ch.akop.homesystem.models.deconz.rest.DeconzSensorResponse;
import ch.akop.homesystem.models.deconz.rest.Specs;
import ch.akop.homesystem.models.deconz.rest.UpdateLightParameters;
import ch.akop.homesystem.models.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.models.devices.sensor.CloseContact;
import ch.akop.homesystem.services.AutomationService;
import ch.akop.homesystem.services.DeviceService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;


@Service
@Slf4j
public class DeconzConnector extends WebSocketClient {

    private final Gson gson;
    private final WebClient webClient;
    private final DeviceService deviceService;

    public DeconzConnector(@Value("${home-automation.deconz.host}") final String host,
                           @Value("${home-automation.deconz.websocket-port}") final int webSocketPort,
                           @Value("${home-automation.deconz.port}") final int port,
                           @Value("${home-automation.deconz.api-key}") final String apikey,
                           final AutomationService automationService,
                           final Gson gson,
                           final DeviceService deviceService) throws URISyntaxException {
        super(new URI("ws://%s:%d".formatted(host, webSocketPort)));

        this.gson = gson;
        this.deviceService = deviceService;
        this.webClient = WebClient.create("http://%s:%d/api/%s/".formatted(host, port, apikey));

        registerDevices();
        this.connect();

        automationService.discoverNewDevices();
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
                    .setId("sensor@" + id)
                    .setLastChange(LocalDateTime.now()));
        }
    }

    private void registerLight(final String id, final DeconzLightResponse light) {
        this.deviceService.registerDevice(
                new Light((bri, duration) -> this.setBrightnessOfLight(id, bri, duration),
                        onOrOff -> {
                        })
                        .setId("light@" + id)
                        .setName(light.getName()));
    }

    private void setBrightnessOfLight(final String id, final Integer bri, final Duration duration) {
        final var response = Specs.setLight(id, new UpdateLightParameters()
                        .setTransitiontime((int) duration.toSeconds() * 10)
                        .setBri(bri)
                        .setOn(bri > 0),
                this.webClient).block();

        log.debug("Set light " + id + " to " + bri + " was status " + response.getStatusCode());
    }


    @Override
    public void onOpen(final ServerHandshake handshakedata) {
        log.info("WebSocket is up and listing.");
    }

    @Override
    public void onMessage(final String message) {
        try {
            this.handleMessage(this.gson.fromJson(message, WebSocketUpdate.class));
        } catch (final Exception e) {
            log.error("Exception occurred with the message:\n{}", message, e);
        }
    }

    private void handleMessage(final WebSocketUpdate update) {
        if (update.getR().equals("sensors")
                && update.getE().equals("changed")
                && update.getState() != null
                && update.getState().getOpen() != null) {
            this.deviceService.getDevice("sensor@" + update.getId(), CloseContact.class)
                    .setOpen(update.getState().getOpen());
        }
    }

    @Override
    public void onClose(final int code, final String reason, final boolean remote) {
        log.info("WS-Connection was closed, because of '{}'. Reconnecting ...", reason);
        this.connect();
    }

    @Override
    public void onError(final Exception ex) {
        log.error("Got exception", ex);
    }
}
