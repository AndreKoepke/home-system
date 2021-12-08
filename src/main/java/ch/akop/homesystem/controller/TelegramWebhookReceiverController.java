package ch.akop.homesystem.controller;

import ch.akop.homesystem.services.impl.TelegramMessageService;
import com.pengrad.telegrambot.BotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> gotWebhookUpdate(@PathVariable final String apikey, @RequestBody final String json) {
        if (!this.telegramMessageService.getBotToken().equals(apikey)) {
            return ResponseEntity.notFound().build();
        }

        try {
            this.telegramMessageService.process(BotUtils.parseUpdate(json));
        } catch (final Exception e) {
            log.error("Failed to handle this payload:\n{}", json, e);
        }

        return ResponseEntity.ok().build();
    }

}
