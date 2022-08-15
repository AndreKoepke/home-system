package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import ch.akop.homesystem.util.SleepUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Observable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
public class DeconzWebsocketService implements WebSocket.Listener {

    private final DeconzConfig deconzConfig;
    private final DeconzConnector deconzConnector;
    private final ObjectMapper objectMapper;

    private LocalDateTime lastContact = LocalDateTime.MIN;
    private StringBuilder messageBuilder = new StringBuilder();
    private CompletableFuture<?> messageCompleteFuture = new CompletableFuture<>();
    private WebSocket webSocket;


    @PostConstruct
    public void setupWebSocketListener() {

        var wsUrl = URI.create("ws://%s:%d/ws".formatted(deconzConfig.getHost(), deconzConfig.getWebsocketPort()));

        getWebSocketCompletableFuture(wsUrl, this)
                .exceptionallyCompose(throwable -> {
                    SleepUtil.sleep(Duration.ofSeconds(30));
                    return getWebSocketCompletableFuture(wsUrl, this);
                })
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
        WebSocket.Listener.super.onOpen(webSocket);

        lastContact = LocalDateTime.now();
        this.webSocket = webSocket;

        Observable.interval(30, 30, TimeUnit.SECONDS)
                .subscribe()
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
    }

    private void checkTimeout() {
        var lastContactSinceSeconds = Duration.between(lastContact, LocalDateTime.now()).toSeconds();

        if (lastContactSinceSeconds > 60) {
            log.error("websocket-timeout", new TimeoutException("No websocket-contact since 60s. Timeout."));
        } else if (lastContactSinceSeconds > 30) {
            log.warn("No websocket-contact since 30s");
            this.webSocket.sendPing(ByteBuffer.wrap(new byte[]{1, 2, 3})));
        }
    }
}
