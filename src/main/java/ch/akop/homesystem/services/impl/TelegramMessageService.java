package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.services.MessageService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SetWebhook;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

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
    private String mainChannel;

    @Setter(AccessLevel.NONE)
    private TelegramBot bot;

    private Subject<String> messages = PublishSubject.create();

    @PostConstruct
    @SneakyThrows
    public void turnBotOn() {
        if (botToken == null || botPath == null) {
            log.info("No telegrambot will be started.");
            return;
        }

        bot = new TelegramBot(botToken);

        SetWebhook request = new SetWebhook().url(botPath);
        var response = bot.execute(request);
        if (!response.isOk()) {
            throw new IllegalStateException(response.description());
        }
    }

    @Override
    public MessageService sendMessageToMainChannel(@Nullable String message) {
        if (mainChannel == null || message == null) {
            return this;
        }

        return sendMessageToUser(message, mainChannel);
    }

    @Override
    public MessageService sendImageToMainChannel(byte @NonNull [] image, @NonNull String caption) {
        if (mainChannel == null) {
            return this;
        }

        return sendImageToUser(image, mainChannel, caption);
    }

    @Override
    public MessageService sendMessageToUser(@Nullable String message, @NonNull String chatId) {
        return sendMessageToUser(message, List.of(chatId));
    }

    @Override
    public MessageService sendMessageToUser(@Nullable String message, @NonNull List<String> chatIds) {
        if (bot != null) {
            chatIds.forEach(chatId -> bot.execute(new SendMessage(chatId, message)));
        }

        return this;
    }

    public MessageService sendImageToUser(byte @NonNull [] image, @NonNull String chatId, @NonNull String text) {
        if (bot != null) {
            bot.execute(new SendPhoto(chatId, image)
                    .caption(text));
        }

        return this;
    }

    public void process(Update update) {
        log.info("Message from {}@{}: {}", update.message().from().firstName(),
                update.message().chat().id(),
                update.message().text());

        if (mainChannel.equals(update.message().chat().id().toString())) {
            messages.onNext(update.message().text());
        }
    }
}
