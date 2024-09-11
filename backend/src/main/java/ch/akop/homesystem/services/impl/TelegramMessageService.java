package ch.akop.homesystem.services.impl;

import static java.util.Optional.ofNullable;

import ch.akop.homesystem.external.openai.OpenAIService;
import ch.akop.homesystem.persistence.model.config.TelegramConfig;
import ch.akop.homesystem.persistence.repository.config.TelegramConfigRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetWebhook;
import io.quarkus.runtime.Startup;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class TelegramMessageService {

  private final TelegramConfigRepository telegramConfigRepository;
  private final OpenAIService openAIService;

  @Setter(AccessLevel.NONE)
  private TelegramBot bot;

  @Getter
  private final Subject<String> messages = PublishSubject.create();

  @PostConstruct
  @Transactional
  @SneakyThrows
  void turnBotOn() {
    var configOpt = ofNullable(telegramConfigRepository.getFirstByOrderByModifiedDesc());
    if (configOpt.isEmpty()) {
      log.info("No TelegramBot will be started.");
      return;
    }
    var config = configOpt.get();

    bot = new TelegramBot.Builder(config.getBotToken())
        .updateListenerSleep(5000)
        .build();

    if (config.getBotPath() != null) {
      log.info("Path configured, setting up webhook");
      setupWebhook(config);
    } else {
      log.info("Path not configured, setting up update-poller");
      deleteWebhook();
      bot.setUpdatesListener(
          updates -> {
            updates.forEach(update -> consumeUpdate(update, config));
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
          },
          exception -> {
            if (exception.response() != null) {
              log.error("Telegram an∆swered with an error. Code: " + exception.response().errorCode()
                  + " \n"
                  + exception.response().description());
            } else {
              log.error("There was an error while talking to Telegram.", exception);
            }
          });
    }
  }

  private void setupWebhook(TelegramConfig config) {
    SetWebhook request = new SetWebhook().url(config.getBotPath() + config.getBotToken());
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

    ofNullable(telegramConfigRepository.getFirstByOrderByModifiedDesc())
        .ifPresent(config -> sendMessageToUser(message, config.getMainChannel()));

    return this;
  }

  public TelegramMessageService sendFunnyMessageToMainChannel(@Nullable String message) {
    if (message == null) {
      return this;
    }

    sendMessageToMainChannel(openAIService.requestText(message));
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

  @Transactional
  public void process(Update update, String transferredApiKey) {
    ofNullable(telegramConfigRepository.getFirstByOrderByModifiedDesc())
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
