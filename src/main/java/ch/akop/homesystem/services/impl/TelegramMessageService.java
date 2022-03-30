package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.services.MessageService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetWebhook;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
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

    private Subject<String> messages = PublishSubject.create();

    @PostConstruct
    @SneakyThrows
    public void turnBotOn() {
        if (this.botToken == null || this.botPath == null) {
            log.info("No telegrambot will be started.");
            return;
        }

        this.bot = new TelegramBot(this.botToken);

        final SetWebhook request = new SetWebhook().url(this.botPath);
        final var response = this.bot.execute(request);
        if (!response.isOk()) {
            throw new IllegalStateException(response.description());
        }
    }

    @Override
    public MessageService sendMessageToUser(final String message) {
        if (this.bot == null) {
            return this;
        }

        this.doorUpdatesTo.forEach(chatId -> this.bot.execute(new SendMessage(chatId, message)));
        return this;
    }

    public void process(final Update update) {
        log.info("Message from {}@{}: {}", update.message().from().firstName(),
                update.message().chat().id(),
                update.message().text());

        this.messages.onNext(update.message().text());
    }
}
