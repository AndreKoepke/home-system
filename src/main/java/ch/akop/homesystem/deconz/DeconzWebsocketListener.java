package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeconzWebsocketListener implements WebSocket.Listener {

    public static final int TIMEOUT_HANDLER_INTERVAL = 30;
    private final DeconzConfig deconzConfig;
    private final DeconzConnector deconzConnector;
    private final ObjectMapper objectMapper;

    private LocalDateTime lastContact = LocalDateTime.MIN;
    private StringBuilder messageBuilder = new StringBuilder();
    private CompletableFuture<?> messageCompleteFuture = new CompletableFuture<>();
    private Disposable timeoutHandler = Disposable.empty();
    private WebSocket webSocket;


    @PostConstruct
    public void setupWebSocketListener() {
        var wsUrl = URI.create("ws://%s:%d/ws".formatted(deconzConfig.getHost(), deconzConfig.getWebsocketPort()));

        //noinspection ResultOfMethodCallIgnored
        Observable.defer(() -> Observable.fromFuture(getWebSocketCompletableFuture(wsUrl, this)))
                .retry()
                .subscribe(webSocket -> this.webSocket = webSocket);
    }

    @PreDestroy
    public void closeConnection() {
        if (webSocket != null) {
            webSocket.abort();
        }

        if (timeoutHandler != null) {
            timeoutHandler.dispose();
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

        timeoutHandler = Observable.interval(TIMEOUT_HANDLER_INTERVAL, TIMEOUT_HANDLER_INTERVAL, TimeUnit.SECONDS)
                .map(aLong -> webSocket)
                .doOnNext(this::checkTimeout)
                .subscribe();
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
        timeoutHandler.dispose();
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
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("Error on WS-Connection", error);
        timeoutHandler.dispose();
        setupWebSocketListener();
    }

    private void checkTimeout(WebSocket webSocket) {
        var lastContactSinceSeconds = Duration.between(lastContact, LocalDateTime.now()).toSeconds();

        if (lastContactSinceSeconds > 60) {
            timeoutHandler.dispose();
            log.error("WS-Connection has no longer contact", new TimeoutException("No websocket-contact since 60s. Timeout."));
            webSocket.abort();
            setupWebSocketListener();
        } else if (lastContactSinceSeconds > TIMEOUT_HANDLER_INTERVAL) {
            log.warn("No websocket-contact since 30s");
            webSocket.sendPing(ByteBuffer.wrap(new byte[]{1, 2, 3}));
        }
    }
}
