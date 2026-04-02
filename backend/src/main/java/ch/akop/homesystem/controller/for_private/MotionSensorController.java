package ch.akop.homesystem.controller.for_private;

import ch.akop.homesystem.controller.dtos.MotionSensorDto;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.models.devices.sensor.Sensor;
import ch.akop.homesystem.persistence.repository.config.MotionSensorConfigRepository;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.MotionSensorService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Path("secured/v1/devices/sensors/motion-sensors")
public class MotionSensorController {

  private final DeviceService deviceService;
  private final MotionSensorConfigRepository motionSensorConfigRepository;
  private final MotionSensorService motionSensorService;

  @GET
  public Stream<MotionSensorDto> getAllSensors() {
    return deviceService.getDevicesOfType(MotionSensor.class)
        .stream()
        .map(MotionSensorDto::from)
        .map(motionSensorDto -> motionSensorConfigRepository
            .findById(motionSensorDto.getName()).map(motionSensorDto::appendConfig)
            .orElse(motionSensorDto));
  }

  @Path("{id}")
  @GET
  public MotionSensorDto getSensor(@PathParam("id") String id) {
    return deviceService.findDeviceById(id, Sensor.class)
        .map(MotionSensorDto::from)
        .map(motionSensorDto -> motionSensorConfigRepository
            .findById(motionSensorDto.getName()).map(motionSensorDto::appendConfig)
            .orElse(motionSensorDto))
        .orElseThrow(() -> new NotFoundException(id));
  }

  @POST
  public void updateConfig(MotionSensorDto.ConfigDto config) {
    motionSensorService.update(config);
  }
}
