package ch.akop.homesystem.controller.for_private;

import ch.akop.homesystem.controller.dtos.AnimationDto;
import ch.akop.homesystem.services.impl.AnimationService;
import java.util.UUID;
import java.util.stream.Stream;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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

  @Path("/start/${animationId}")
  @POST
  public void startAnimation(UUID animationId) {
    animationService.playAnimation(animationId);
  }
}
