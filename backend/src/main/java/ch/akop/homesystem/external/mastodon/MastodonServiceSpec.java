package ch.akop.homesystem.external.mastodon;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

@Path("/api")
@RegisterRestClient
@RegisterClientHeaders(MastodonHeader.class)
public interface MastodonServiceSpec {

  @POST
  @Path("v1/statuses")
  void postStatus(@FormParam("status") String status,
      @FormParam("media_ids[]") String mediaIds,
      @FormParam("language") String language,
      @FormParam("visibility") String visibility);


  @POST
  @Path("v2/media")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  MediaCreateResponse postImage(@RestForm("file") byte[] file, @RestForm("fileName") String fileName);

}
