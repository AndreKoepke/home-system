package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.LightDto;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.impl.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@ServerEndpoint("/secured/ws/v1/devices/lights")
@RequiredArgsConstructor
public class LightsChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;

  @Getter
  private final ObjectMapper objectMapper;

  @ConsumeEvent(value = "devices/lights/update", blocking = true)
  void updateLight(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, SimpleLight.class)
        .map(LightDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {
    registerSession(session);
    sendAllLightsToSession(session.getId());
  }

  @OnClose
  public void onClose(Session session) {
    deregisterSession(session.getId());
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    deregisterSession(session.getId());
  }

  @SneakyThrows
  private void sendAllLightsToSession(String sessionId) {
    deviceService.getDevicesOfType(SimpleLight.class)
        .stream()
        .map(LightDto::from)
        .forEach(motionSensor -> sendMessage(sessionId, motionSensor));
  }
}
