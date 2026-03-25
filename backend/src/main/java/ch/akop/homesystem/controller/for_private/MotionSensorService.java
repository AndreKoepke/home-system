package ch.akop.homesystem.controller.for_private;

import ch.akop.homesystem.controller.dtos.SensorDto;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.Sensor;
import ch.akop.homesystem.persistence.repository.config.MotionSensorConfigRepository;
import ch.akop.homesystem.services.impl.DeviceService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Path("sensors/motion-sensors")
public class MotionSensorService {

  private final DeviceService deviceService;
  private final MotionSensorConfigRepository motionSensorConfigRepository;

  @GET
  public Stream<SensorDto> getAllSensors() {
    return deviceService.getDevicesOfType(MotionSensor.class)
        .stream()
        .map(SensorDto::from)
        .map(sensorDto -> motionSensorConfigRepository
            .findById(sensorDto.getName()).map(sensorDto::appendConfig)
            .orElse(sensorDto));
  }

  @Path("{id}")
  @GET
  public SensorDto getSensor(@PathParam("id") String id) {
    return deviceService.findDeviceById(id, Sensor.class)
        .map(SensorDto::from)
        .map(sensorDto -> motionSensorConfigRepository
            .findById(sensorDto.getName()).map(sensorDto::appendConfig)
            .orElse(sensorDto))
        .orElseThrow(() -> new NotFoundException(id));
  }
}
