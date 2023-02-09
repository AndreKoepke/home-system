package ch.akop.homesystem.external.mastodon;


import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
