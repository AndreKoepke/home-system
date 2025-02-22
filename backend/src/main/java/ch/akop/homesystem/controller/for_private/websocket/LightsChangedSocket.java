package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.LightDto;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.impl.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.ConsumeEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
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
    log.info("Opening session: {}", session.getId());
    registerSession(session);
    sendAllLightsToSession(session.getId());
  }

  @OnClose
  public void onClose(Session session) {
    log.info("Close session: {}", session.getId());
    deregisterSession(session.getId());
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    log.error("Error on session: {}", session.getId(), throwable);
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
