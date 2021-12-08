package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.services.MessageService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetWebhook;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
@Getter
@Setter
@Slf4j
@ConfigurationProperties(prefix = "home-automation.telegram")
public class TelegramMessageService implements MessageService {

    private String botToken;
    private String botUsername;
    private String botPath;
    private List<String> doorUpdatesTo;

    @Setter(AccessLevel.NONE)
    private TelegramBot bot;

    @PostConstruct
    @SneakyThrows
    public void turnBotOn() {
        this.bot = new TelegramBot(this.botToken);

        final SetWebhook request = new SetWebhook().url(this.botPath);
        final var response = this.bot.execute(request);
        if (!response.isOk()) {
            throw new IllegalStateException(response.description());
        }
    }

    @Override
    public void sendMessageToUser(final String message) {
        this.doorUpdatesTo.forEach(chatId -> this.bot.execute(new SendMessage(chatId, message)));
    }

    public void process(final Update update) {
        log.info("Message from {}@{}: {}", update.message().from().firstName(),
                update.message().chat().id(),
                update.message().text());
    }
}
