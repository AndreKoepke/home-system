package ch.akop.homesystem.deconz.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient
public interface DeconzService {

  @GET
  @Path("sensors")
  Sensors getAllSensors();

  @GET
  @Path("/lights")
  Lights getAllLights();

  @GET
  @Path("/groups")
  Groups getAllGroups();

  @PUT
  @Path("/lights/{id}/state")
  void updateLight(@PathParam("id") String id, State newState);

  @PUT
  @Path("/groups/{groupId}/scenes/{sceneId}/recall")
  void activateScene(@PathParam("groupId") String groupId, @PathParam("sceneId") String sceneId);

}
