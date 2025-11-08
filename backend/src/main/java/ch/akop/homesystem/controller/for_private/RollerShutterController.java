package ch.akop.homesystem.controller.for_private;


import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.RollerShutterService;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import lombok.RequiredArgsConstructor;


@Path("secured/v1/devices/roller-shutters/")
@RequiredArgsConstructor
public class RollerShutterController {

  private final DeviceService deviceService;
  private final RollerShutterService rollerShutterService;


  @POST
  @Path("open-all")
  public void openAllRollerShutters() {
    deviceService.getDevicesOfType(RollerShutter.class)
        .forEach(rollerShutter -> rollerShutter.open("open-all").subscribe());
  }

  @POST
  @Path("close-all")
  public void closeAllRollerShutters() {
    deviceService.getDevicesOfType(RollerShutter.class)
        .forEach(rollerShutter -> rollerShutter.close("close-all").subscribe());
  }

  @POST
  @Path("start-calculating-again")
  public void startCalculatingAgain() {
    rollerShutterService.startCalculatingAgain();
  }

  @POST
  @Path("{id}/block")
  public void block(@PathParam("id") String id) {
    rollerShutterService.block(id);
  }

  @POST
  @Path("{id}/unblock")
  public void startCalculatingAgain(@PathParam("id") String id) {
    rollerShutterService.unblock(id);
  }

  @POST
  @Path("{id}/lift/to/{lift}")
  public void setLift(@PathParam("id") String id, @PathParam("lift") int lift) {
    rollerShutterService.setLift(id, lift);
  }

  @POST
  @Path("{id}/lift/to/{tilt}")
  public void setTilt(@PathParam("id") String id, @PathParam("tilt") int tilt) {
    rollerShutterService.setTilt(id, tilt);
  }
}
