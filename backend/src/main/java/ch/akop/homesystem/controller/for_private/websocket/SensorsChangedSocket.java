package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.ActorDto;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.services.impl.DeviceService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
@ServerEndpoint("/secured/ws/v1/devices/sensors")
@RequiredArgsConstructor
public class SensorsChangedSocket {

  private final DeviceService deviceService;
  private final ObjectMapper objectMapper;

  Map<String, Session> sessions = new ConcurrentHashMap<>();

  @ConsumeEvent(value = "devices/sensors/update", blocking = true)
  void updateSensor(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, MotionSensor.class)
        .map(ActorDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {
    log.info("Opening session: {}", session.getId());
    sessions.put(session.getId(), session);
    sendAllSensorsToSession(session);
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

  @SneakyThrows
  private void sendAllSensorsToSession(Session session) {
    deviceService.getDevicesOfType(MotionSensor.class)
        .forEach(motionSensor -> {
          try {
            var payload = objectMapper.writeValueAsString(motionSensor);
            session.getAsyncRemote().sendObject(payload, result -> {
              if (result.getException() != null) {
                log.error("Error while sending message", result.getException());
              }
            });
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }
}
