package ch.akop.homesystem.controller.for_public;


import ch.akop.homesystem.services.impl.ImageCreatorService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;


@Path("/v1/images")
@RequiredArgsConstructor
public class ImageController {

  private final ImageCreatorService imageCreatorService;

  @Path("daily.jpg")
  @GET
  @PermitAll
  public RestResponse<byte[]> getDailyImage() {
    var image = imageCreatorService.getLastImage();
    imageCreatorService.increaseDownloadCounter(image.getCreated());

    return ResponseBuilder.ok(image.getImage(), new MediaType("image", "jpeg"))
        .tag(image.getCreated().toString())
        .header("prompt", image.getPrompt())
        .cacheControl(getLongTermCache())
        .build();
  }

  @Path("prompt")
  @GET
  @PermitAll
  public String getPrompt() {
    return imageCreatorService.getLastPrompt();
  }

  private CacheControl getLongTermCache() {
    var cacheControl = new CacheControl();
    cacheControl.setMaxAge((int) Duration.ofHours(10).toSeconds());
    cacheControl.setMustRevalidate(false);
    return cacheControl;
  }

}
