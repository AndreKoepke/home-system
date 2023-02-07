package ch.akop.homesystem.external.openai;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/v1")
@RegisterRestClient
@RegisterClientHeaders(OpenAIHeader.class)
public interface OpenAIServiceSpec {

    @POST
    @Path("images/generations")
    Response requestImage(ImageRequest request);

}
