package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.authentication.AuthenticationService;
import ch.akop.homesystem.controller.dtos.SensorDto;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.persistence.repository.config.MotionSensorConfigRepository;
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
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@WebSocket(path = "/secured/ws/v1/devices/sensors/motion-sensors")
@RequiredArgsConstructor
public class MotionSensorsChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;
  private final MotionSensorConfigRepository motionSensorConfigRepository;

  @Getter
  private final AuthenticationService authenticationService;

  @Getter
  private final ObjectMapper objectMapper;

  @Transactional
  @ConsumeEvent(value = "devices/sensors/update", blocking = true)
  void updateSensor(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, MotionSensor.class)
        .map(SensorDto::from)
        .map(sensorDto -> motionSensorConfigRepository
            .findByName(sensorDto.getName())
            .map(sensorDto::appendConfig)
            .orElse(sensorDto))
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(WebSocketConnection session) {

  }

  @OnTextMessage
  @Transactional
  public void onMessage(String message, WebSocketConnection session) {
    if (registerSession(session, message)) {
      sendAllSensorsToSession(session.id());
    }
  }

  @OnClose
  public void onClose(WebSocketConnection session) {
    deregisterSession(session.id());
  }

  @OnError
  public void onError(WebSocketConnection session, Throwable throwable) {
    log.error("Error on session: {}", session.id(), throwable);
    deregisterSession(session.id());
  }

  @SneakyThrows
  private void sendAllSensorsToSession(String sessionId) {
    deviceService.getDevicesOfType(MotionSensor.class)
        .stream()
        .map(SensorDto::from)
        .map(sensorDto -> motionSensorConfigRepository
            .findByName(sensorDto.getName())
            .map(sensorDto::appendConfig)
            .orElse(sensorDto))
        .forEach(motionSensor -> sendMessage(sessionId, motionSensor));
  }
}
