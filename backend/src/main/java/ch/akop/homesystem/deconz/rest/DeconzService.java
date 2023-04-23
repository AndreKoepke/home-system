package ch.akop.homesystem.deconz.rest;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
