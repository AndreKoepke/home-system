package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.ActorDto;
import ch.akop.homesystem.deconz.websocket.Sensor;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.services.impl.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.ConsumeEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@ServerEndpoint("/secured/ws/v1/devices")
@RequiredArgsConstructor
public class DeviceChangedSocket {

  private final DeviceService deviceService;
  private final ObjectMapper objectMapper;

  Map<String, Session> sessions = new ConcurrentHashMap<>();

  @ConsumeEvent(value = "devices/lights/update", blocking = true)
  void updateDevice(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, SimpleLight.class)
        .map(ActorDto::from)
        .ifPresent(this::broadcast);
  }

  @ConsumeEvent(value = "devices/sensora/update", blocking = true)
  void updateDevice(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, Sensor.class)
        .map(ActorDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {
    log.info("Opening session: {} @ {}", session.getId());
    sessions.put(session.getId(), session);
  }

  @OnClose
  public void onClose(Session session) {
    log.info("Close session: {}", session.getId());
    sessions.remove(session.getId());
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    log.error("Error on session: {}", session.getId(), throwable);
    sessions.remove(session.getId());
  }

  @SneakyThrows
  private void broadcast(Object message) {

    if (sessions.isEmpty()) {
      return;
    }

    var payload = objectMapper.writeValueAsString(message);
    log.info("Sending message {}", payload);

    sessions.values().forEach(s -> s.getAsyncRemote().sendObject(payload, result -> {
      if (result.getException() != null) {
        log.error("Error while sending message", result.getException());
      }
    }));
  }
}
