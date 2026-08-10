package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.authentication.AuthenticationService;
import ch.akop.homesystem.controller.dtos.LightDto;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.impl.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@WebSocket(path = "/secured/ws/v1/devices/lights")
@RequiredArgsConstructor
public class LightsChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;
  @Inject
  WebSocketConnection webSocketConnection;

  @Getter
  private final AuthenticationService authenticationService;

  @Getter
  private final ObjectMapper objectMapper;

  @ConsumeEvent(value = "devices/lights/update", blocking = true)
  void updateLight(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, SimpleLight.class)
        .map(LightDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(WebSocketConnection session) {

  }

  @OnTextMessage
  public void onMessage(String message, WebSocketConnection connection) {
    if (registerSession(connection, message)) {
      sendAllLightsToSession(webSocketConnection.id());
    }
  }

  @OnClose
  public void onClose(WebSocketConnection session) {
    deregisterSession(session.id());
  }

  @OnError
  public void onError(WebSocketConnection session, Throwable throwable) {
    deregisterSession(session.id());
  }

  @SneakyThrows
  private void sendAllLightsToSession(String sessionId) {
    deviceService.getDevicesOfType(SimpleLight.class)
        .stream()
        .map(LightDto::from)
        .forEach(motionSensor -> sendMessage(sessionId, motionSensor));
  }
}
