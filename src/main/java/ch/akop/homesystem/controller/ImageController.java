package ch.akop.homesystem.controller;


import ch.akop.homesystem.services.impl.ImageCreatorService;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import java.time.Duration;


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

        var cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) Duration.ofHours(10).toSeconds());
        cacheControl.setMustRevalidate(false);

        return ResponseBuilder.ok(image.getImage(), new MediaType("image", "jpeg"))
                .tag(image.getCreated().toString())
                .header("prompt", image.getPrompt())
                .cacheControl(cacheControl)
                .build();
    }


}
