package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.ActorDto;
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
@ServerEndpoint("/secured/ws/v1/devices/actors")
@RequiredArgsConstructor
public class ActorsChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;

  @Getter
  private final ObjectMapper objectMapper;

  @ConsumeEvent(value = "devices/lights/update", blocking = true)
  void updateLight(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, SimpleLight.class)
        .map(ActorDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {
    log.info("Opening session: {}", session.getId());
    registerSession(session);
    sendAllLightsToSession(session);
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
  private void sendAllLightsToSession(Session session) {
    deviceService.getDevicesOfType(SimpleLight.class)
        .stream()
        .map(ActorDto::from)
        .forEach(motionSensor -> sendMessage(session, motionSensor));
  }
}
