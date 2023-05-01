package ch.akop.homesystem.deconz;

import static java.util.Optional.ofNullable;

import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.persistence.repository.config.DeconzConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
@Startup
public class DeconzWebsocketListener implements WebSocket.Listener {

  public static final Duration TIMEOUT_HANDLER_INTERVAL = Duration.ofSeconds(15);
  private final DeconzConnector deconzConnector;
  private final ObjectMapper objectMapper;
  private final DeconzConfigRepository deconzConfigRepository;
  private final Vertx vertx;
  private final ManagedExecutor executor;

  private LocalDateTime lastContact = LocalDateTime.MIN;
  private StringBuilder messageBuilder = new StringBuilder();
  private CompletableFuture<?> messageCompleteFuture = new CompletableFuture<>();
  private Long timeoutHandler;
  private WebSocket webSocket;
  private Scheduler blockingScheduler;

  @PostConstruct
  @Transactional
  void setupWebSocketListener() {

    if (blockingScheduler == null) {
      blockingScheduler = RxHelper.blockingScheduler(vertx);
    }

    ofNullable(deconzConfigRepository.getFirstByOrderByModifiedDesc())
        .ifPresent(config -> {
          var wsUrl = URI.create("ws://%s:%d/ws".formatted(config.getHost(), config.getWebsocketPort()));

          //noinspection ResultOfMethodCallIgnored
          Observable.defer(() -> Observable.fromFuture(getWebSocketCompletableFuture(wsUrl, this)))
              .subscribeOn(blockingScheduler)
              .retry()
              .subscribe(webSocket -> this.webSocket = webSocket);
        });
  }

  @PreDestroy
  public void closeConnection() {
    if (webSocket != null) {
      webSocket.abort();
    }

    if (timeoutHandler != null) {
      vertx.cancelTimer(timeoutHandler);
    }
  }

  private static CompletableFuture<WebSocket> getWebSocketCompletableFuture(URI wsUrl, WebSocket.Listener listener) {
    return HttpClient.newHttpClient()
        .newWebSocketBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .buildAsync(wsUrl, listener);
  }

  @Override
  public void onOpen(WebSocket webSocket) {
    log.info("WS-Connection is established.");
    lastContact = LocalDateTime.now();
    webSocket.request(1);

    timeoutHandler = vertx.setPeriodic(TIMEOUT_HANDLER_INTERVAL.toMillis(), ignore -> checkTimeout(webSocket));

  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    messageBuilder.append(data);
    webSocket.request(1);
    lastContact = LocalDateTime.now();

    if (last) {
      handleCompleteMessage(messageBuilder.toString());
      messageBuilder = new StringBuilder();
      messageCompleteFuture.complete(null);
      var oldFuture = messageCompleteFuture;
      messageCompleteFuture = new CompletableFuture<>();
      return oldFuture;
    }

    return messageCompleteFuture;
  }

  @Override
  public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    log.error("WS-Connection closed, because of '{}'", reason);
    vertx.cancelTimer(timeoutHandler);
    executor.runAsync(this::setupWebSocketListener);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
    webSocket.request(1);
    webSocket.sendPing(message);
    lastContact = LocalDateTime.now();
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    webSocket.request(1);
    lastContact = LocalDateTime.now();
    return CompletableFuture.completedFuture(null);
  }

  private void handleCompleteMessage(String message) {
    try {
      var parsed = objectMapper.readValue(message, WebSocketUpdate.class);
      deconzConnector.handleMessage(parsed);
    } catch (Exception e) {
      log.error("There was a problem while parsing message:\n{}", message, e);
    }
  }

  @Override
  @Transactional
  public void onError(WebSocket webSocket, Throwable error) {
    log.error("Error on WS-Connection", error);
    vertx.cancelTimer(timeoutHandler);
  }

  private void checkTimeout(WebSocket webSocket) {
    var lastContact = Duration.between(this.lastContact, LocalDateTime.now());

    if (lastContact.toSeconds() > 60) {
      vertx.cancelTimer(timeoutHandler);
      log.error("WS-Connection has no longer contact", new TimeoutException("No websocket-contact since 60s. Timeout."));
      webSocket.abort();
    } else if (lastContact.compareTo(TIMEOUT_HANDLER_INTERVAL.multipliedBy(2)) > 0) {
      log.warn("No websocket-contact since " + lastContact.toSeconds() + "s");
      webSocket.sendPing(ByteBuffer.wrap(new byte[]{1, 2, 3}));
    }
  }
}
