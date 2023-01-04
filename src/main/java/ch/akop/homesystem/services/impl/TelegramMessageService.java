package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.persistence.repository.config.TelegramConfigRepository;
import ch.akop.homesystem.services.MessageService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SetWebhook;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramMessageService implements MessageService {

    private final TelegramConfigRepository telegramConfigRepository;

    @Setter(AccessLevel.NONE)
    private TelegramBot bot;

    @Getter
    private final Subject<String> messages = PublishSubject.create();

    @PostConstruct
    @SneakyThrows
    public void turnBotOn() {
        var configOpt = telegramConfigRepository.findFirstByOrderByModifiedDesc();
        if (configOpt.isEmpty()) {
            log.info("No telegrambot will be started.");
            return;
        }

        bot = new TelegramBot(configOpt.get().getBotToken());

        SetWebhook request = new SetWebhook().url(configOpt.get().getBotPath());
        var response = bot.execute(request);
        if (!response.isOk()) {
            throw new IllegalStateException(response.description());
        }
    }

    @Override
    @Transactional
    public MessageService sendMessageToMainChannel(@Nullable String message) {

        if (message == null) {
            return this;
        }

        telegramConfigRepository.findFirstByOrderByModifiedDesc()
                .ifPresent(config -> sendMessageToUser(message, config.getMainChannel()));

        return this;
    }

    @Override
    public MessageService sendImageToMainChannel(byte @NonNull [] image, @NonNull String caption) {
        telegramConfigRepository.findFirstByOrderByModifiedDesc()
                .ifPresent(config -> sendImageToUser(image, config.getMainChannel(), caption));
        return this;
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
            bot.execute(new SendPhoto(chatId, image).caption(text));
        }

        return this;
    }

    @Transactional
    public void process(Update update, String transferredApiKey) {
        telegramConfigRepository.findFirstByOrderByModifiedDesc()
                .filter(telegramConfig -> telegramConfig.getBotToken().equals(transferredApiKey))
                .ifPresent(config -> {
                    log.info("Message from {}@{}: {}", update.message().from().firstName(),
                            update.message().chat().id(),
                            update.message().text());

                    if (config.getMainChannel().equals(update.message().chat().id().toString())
                            && update.message().text() != null) {
                        messages.onNext(update.message().text());
                    }
                });
    }
}
