package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.ActorDto;
import ch.akop.homesystem.controller.dtos.SensorDto;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
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
@ServerEndpoint("/secured/ws/v1/devices/sensors")
@RequiredArgsConstructor
public class SensorsChangedSocket extends AbstractBaseSocket {

  private final DeviceService deviceService;

  @Getter
  private final ObjectMapper objectMapper;

  @ConsumeEvent(value = "devices/sensors/update", blocking = true)
  void updateSensor(String updatedDeviceId) {
    deviceService.findDeviceById(updatedDeviceId, MotionSensor.class)
        .map(ActorDto::from)
        .ifPresent(this::broadcast);
  }

  @OnOpen
  public void onOpen(Session session) {
    log.info("Opening session: {}", session.getId());
    registerSession(session);
    sendAllSensorsToSession(session);
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
  private void sendAllSensorsToSession(Session session) {
    deviceService.getDevicesOfType(MotionSensor.class)
        .stream()
        .map(SensorDto::from)
        .forEach(motionSensor -> sendMessage(session, motionSensor));
  }
}
