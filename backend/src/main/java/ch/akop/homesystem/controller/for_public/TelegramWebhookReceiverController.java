package ch.akop.homesystem.controller.for_public;

import ch.akop.homesystem.services.impl.TelegramMessageService;
import com.pengrad.telegrambot.utility.BotUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
@Path("/telegram")
public class TelegramWebhookReceiverController {

  private final TelegramMessageService telegramMessageService;

  @Path("{apikey}")
  @POST
  @PermitAll
  public void gotWebhookUpdate(@PathParam("apikey") String apikey, String json) {
    try {
      telegramMessageService.process(BotUtils.parseUpdate(json), apikey);
    } catch (Exception e) {
      log.error("Failed to handle this payload:\n{}", json, e);
    }
  }
}
