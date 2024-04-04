package ch.akop.homesystem.controller;

import javax.annotation.security.PermitAll;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Path("/zones/{apikey}")
public class ZoneController {

  @Path("{zoneId}/entered")
  @POST
  @PermitAll
  public void enteredZone(@PathParam("apikey") String apikey, @PathParam("zoneId") String zoneId) {
    log.info("apiKey=" + apikey + " zoneId=" + zoneId);
  }
}
