package ch.akop.homesystem.controller.for_private;

import ch.akop.homesystem.controller.dtos.AnimationDto;
import ch.akop.homesystem.services.impl.AnimationService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@Path("secured/v1/animations")
@RequiredArgsConstructor
public class AnimationController {

  private final AnimationService animationService;

  @Path("/")
  @GET
  public Stream<AnimationDto> getAllLights() {
    return animationService.getAllAnimations().stream()
        .map(AnimationDto::from);
  }

  @Path("/start/{animationId}")
  @POST
  public void startAnimation(@PathParam("animationId") UUID animationId) {
    animationService.playAnimation(animationId);
  }
}
