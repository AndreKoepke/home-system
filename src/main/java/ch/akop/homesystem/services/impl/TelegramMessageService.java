package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.persistence.model.config.TelegramConfig;
import ch.akop.homesystem.persistence.repository.config.TelegramConfigRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SetWebhook;
import io.quarkus.runtime.Startup;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.List;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class TelegramMessageService {

    private final TelegramConfigRepository telegramConfigRepository;

    @Setter(AccessLevel.NONE)
    private TelegramBot bot;

    @Getter
    private final Subject<String> messages = PublishSubject.create();

    @PostConstruct
    @Transactional
    @SneakyThrows
    void turnBotOn() {
        var configOpt = telegramConfigRepository.findFirstByOrderByModifiedDesc();
        if (configOpt.isEmpty()) {
            log.info("No telegrambot will be started.");
            return;
        }
        var config = configOpt.get();

        bot = new TelegramBot.Builder(config.getBotToken())
                .updateListenerSleep(5000)
                .build();

        if (config.getBotPath() != null) {
            setupWebhook(config);
        } else {
            deleteWebhook();
            bot.setUpdatesListener(updates -> {
                updates.forEach(update -> consumeUpdate(update, config));
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            });
        }
    }

    private void setupWebhook(TelegramConfig config) {
        SetWebhook request = new SetWebhook().url(config.getBotPath());
        var response = bot.execute(request);
        if (!response.isOk()) {
            throw new IllegalStateException(response.description());
        }
    }

    private void deleteWebhook() {
        DeleteWebhook request = new DeleteWebhook().dropPendingUpdates(true);
        bot.execute(request);
    }

    @Transactional
    public TelegramMessageService sendMessageToMainChannel(@Nullable String message) {

        if (message == null) {
            return this;
        }

        telegramConfigRepository.findFirstByOrderByModifiedDesc()
                .ifPresent(config -> sendMessageToUser(message, config.getMainChannel()));

        return this;
    }


    public TelegramMessageService sendImageToMainChannel(byte @NonNull [] image, @NonNull String caption) {
        telegramConfigRepository.findFirstByOrderByModifiedDesc()
                .ifPresent(config -> sendImageToUser(image, config.getMainChannel(), caption));
        return this;
    }


    public TelegramMessageService sendMessageToUser(@Nullable String message, @NonNull String chatId) {
        return sendMessageToUser(message, List.of(chatId));
    }


    public TelegramMessageService sendMessageToUser(@Nullable String message, @NonNull List<String> chatIds) {
        if (bot != null) {
            chatIds.forEach(chatId -> bot.execute(new SendMessage(chatId, message)));
        }

        return this;
    }

    public TelegramMessageService sendImageToUser(byte @NonNull [] image, @NonNull String chatId, @NonNull String text) {
        if (bot != null) {
            bot.execute(new SendPhoto(chatId, image).caption(text));
        }

        return this;
    }

    @Transactional
    public void process(Update update, String transferredApiKey) {
        telegramConfigRepository.findFirstByOrderByModifiedDesc()
                .filter(telegramConfig -> telegramConfig.getBotPath() != null)
                .filter(telegramConfig -> telegramConfig.getBotToken().equals(transferredApiKey))
                .ifPresent(config -> consumeUpdate(update, config));
    }

    private void consumeUpdate(@Nullable Update update, @NonNull TelegramConfig config) {

        if (update == null) {
            return;
        }

        log.info("Message from {}@{}: {}", update.message().from().firstName(),
                update.message().chat().id(),
                update.message().text());

        if (config.getMainChannel().equals(update.message().chat().id().toString())
                && update.message().text() != null) {
            messages.onNext(update.message().text());
        }
    }
}
