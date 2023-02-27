package ch.akop.homesystem.controller;

import ch.akop.homesystem.services.impl.TelegramMessageService;
import com.pengrad.telegrambot.BotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;


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
