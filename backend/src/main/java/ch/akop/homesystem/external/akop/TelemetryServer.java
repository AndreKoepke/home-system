package ch.akop.homesystem.external.akop;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/telemetry")
@RegisterRestClient
public interface TelemetryServer {

  @POST
  @Path("sync")
  SyncAck sync();


  @POST
  @Path("heartbeat")
  void heartbeat(Heartbeat heartbeat);
}
