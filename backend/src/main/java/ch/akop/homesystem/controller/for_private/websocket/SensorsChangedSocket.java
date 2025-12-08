package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.SensorDto;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
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
@ServerEndpoint("/secured/ws/v1/devices/sensors")
@RequiredArgsConstructor
public class SensorsChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;

  @Getter
  private final ObjectMapper objectMapper;

  @ConsumeEvent(value = "devices/sensors/update", blocking = true)
  void updateSensor(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, MotionSensor.class)
        .map(SensorDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {
    registerSession(session);
    sendAllSensorsToSession(session.getId());
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
  private void sendAllSensorsToSession(String sessionId) {
    deviceService.getDevicesOfType(MotionSensor.class)
        .stream()
        .map(SensorDto::from)
        .forEach(motionSensor -> sendMessage(sessionId, motionSensor));
  }
}
