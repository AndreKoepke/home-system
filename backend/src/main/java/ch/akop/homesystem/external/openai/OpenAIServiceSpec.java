package ch.akop.homesystem.external.openai;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v1")
@RegisterRestClient
@RegisterClientHeaders(OpenAIHeader.class)
public interface OpenAIServiceSpec {

  @POST
  @Path("images/generations")
  ImageGenerationResponse requestImage(ImageRequest request);

  @POST
  @Path("chat/completions")
  TextGenerationResponse textCompletion(TextGenerationParameter request);

}
