package ch.akop.homesystem.controller.for_private;

import ch.akop.homesystem.controller.dtos.TimerDto;
import ch.akop.homesystem.services.impl.TimerService;
import java.util.stream.Stream;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
