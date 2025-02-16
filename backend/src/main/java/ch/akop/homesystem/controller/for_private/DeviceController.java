package ch.akop.homesystem.controller.for_private;


import ch.akop.homesystem.controller.dtos.LightDto;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
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
  public Stream<LightDto> getAllLights() {
    return deviceService.getDevicesOfType(SimpleLight.class)
        .stream()
        .map(LightDto::from);
  }

  @Path("lights/{id}")
  @GET
  public LightDto getAllLights(@PathParam("id") String id) {
    return deviceService.findDeviceById(id, SimpleLight.class)
        .map(LightDto::from)
        .orElseThrow(() -> new NotFoundException(id));
  }
}
