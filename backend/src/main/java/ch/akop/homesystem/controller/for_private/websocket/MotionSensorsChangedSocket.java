package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.authentication.AuthenticationService;
import ch.akop.homesystem.controller.dtos.SensorDto;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.persistence.repository.config.MotionSensorConfigRepository;
import ch.akop.homesystem.services.impl.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@ServerEndpoint("/secured/ws/v1/devices/sensors/motion-sensors")
@RequiredArgsConstructor
public class MotionSensorsChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;
  private final MotionSensorConfigRepository motionSensorConfigRepository;

  @Getter
  private final AuthenticationService authenticationService;

  @Getter
  private final ObjectMapper objectMapper;

  @ConsumeEvent(value = "devices/sensors/update", blocking = true)
  void updateSensor(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, MotionSensor.class)
        .map(SensorDto::from)
        .map(sensorDto -> motionSensorConfigRepository
            .findById(sensorDto.getName()).map(sensorDto::appendConfig)
            .orElse(sensorDto))
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {

  }

  @OnMessage
  public void onMessage(byte[] message, Session session) {
    if (registerSession(session, message)) {
      sendAllSensorsToSession(session.getId());
    }
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
        .map(sensorDto -> motionSensorConfigRepository
            .findById(sensorDto.getName()).map(sensorDto::appendConfig)
            .orElse(sensorDto))
        .forEach(motionSensor -> sendMessage(sessionId, motionSensor));
  }
}
