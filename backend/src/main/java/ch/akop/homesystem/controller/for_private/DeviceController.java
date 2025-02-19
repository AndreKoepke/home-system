package ch.akop.homesystem.controller.for_private;


import ch.akop.homesystem.controller.dtos.ActorDto;
import ch.akop.homesystem.controller.dtos.SensorDto;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.Sensor;
import ch.akop.homesystem.services.impl.DeviceService;
import java.util.stream.Stream;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import lombok.RequiredArgsConstructor;


@Path("secured/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

  private final DeviceService deviceService;

  @Path("lights")
  @GET
  public Stream<ActorDto> getAllLights() {
    return deviceService.getDevicesOfType(SimpleLight.class)
        .stream()
        .map(ActorDto::from);
  }

  @Path("lights/{id}")
  @GET
  public ActorDto getLight(@PathParam("id") String id) {
    return deviceService.findDeviceById(id, SimpleLight.class)
        .map(ActorDto::from)
        .orElseThrow(() -> new NotFoundException(id));
  }

  @Path("sensors")
  @GET
  public Stream<SensorDto> getAllSensors() {
    return deviceService.getDevicesOfType(Sensor.class)
        .stream()
        .map(SensorDto::from);
  }

  @Path("sensors/{id}")
  @GET
  public SensorDto getSensor(@PathParam("id") String id) {
    return deviceService.findDeviceById(id, Sensor.class)
        .map(SensorDto::from)
        .orElseThrow(() -> new NotFoundException(id));
  }
}
