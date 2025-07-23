package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.RollerShutterDto;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
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
@ServerEndpoint("/secured/ws/v1/devices/roller-shutters")
@RequiredArgsConstructor
public class RollerShutterChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;

  @Getter
  private final ObjectMapper objectMapper;

  @ConsumeEvent(value = "devices/roller-shutters/update", blocking = true)
  void updateLight(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, RollerShutter.class)
        .map(RollerShutterDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {
    registerSession(session);
    sendAllRollerShuttersToSession(session.getId());
  }

  @OnClose
  public void onClose(Session session) {
    deregisterSession(session.getId());
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    log.error("Error on session: {}", session.getId(), throwable);
    deregisterSession(session.getId());
  }

  @SneakyThrows
  private void sendAllRollerShuttersToSession(String sessionId) {
    deviceService.getDevicesOfType(RollerShutter.class)
        .stream()
        .map(RollerShutterDto::from)
        .forEach(rollerShutter -> sendMessage(sessionId, rollerShutter));
  }
}
