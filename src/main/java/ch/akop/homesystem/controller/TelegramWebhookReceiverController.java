package ch.akop.homesystem.controller;

import ch.akop.homesystem.services.impl.TelegramMessageService;
import com.pengrad.telegrambot.BotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookReceiverController {

    private final TelegramMessageService telegramMessageService;

    @PostMapping(path = "/telegram/{apikey}")
    public void gotWebhookUpdate(@PathVariable String apikey, @RequestBody String json) {
        try {
            telegramMessageService.process(BotUtils.parseUpdate(json), apikey);
        } catch (Exception e) {
            log.error("Failed to handle this payload:\n{}", json, e);
        }
    }
}
