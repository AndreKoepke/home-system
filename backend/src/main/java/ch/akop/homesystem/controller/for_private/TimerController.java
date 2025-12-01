package ch.akop.homesystem.controller.for_private;

import ch.akop.homesystem.controller.dtos.TimerDto;
import ch.akop.homesystem.services.impl.TimerService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@Path("secured/v1/timer")
@RequiredArgsConstructor
public class TimerController {

  private final TimerService timerService;

  @GET
  public Stream<TimerDto> getAllConfigs() {
    return timerService.findAll()
        .stream()
        .map(TimerDto::from);
  }

  @POST
  public void update(TimerDto dto) {
    timerService.update(dto.toObject());
  }
}
