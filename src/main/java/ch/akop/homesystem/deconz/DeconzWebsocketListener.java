package ch.akop.homesystem.deconz;

import ch.akop.homesystem.deconz.websocket.WebSocketUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;

@Slf4j
@RequiredArgsConstructor
public class DeconzWebsocketListener extends StompSessionHandlerAdapter {

    private final DeconzConnector deconzConnector;

    @Override
    public void afterConnected(StompSession session, @NotNull StompHeaders connectedHeaders) {
        log.info("WebSocket is connected");
        session.subscribe("/", this);
    }

    @NotNull
    @Override
    public Type getPayloadType(@NotNull StompHeaders headers) {
        return WebSocketUpdate.class;
    }

    @Override
    public void handleFrame(@NotNull StompHeaders headers, Object rawPayload) {
        var payload = (WebSocketUpdate) rawPayload;
        deconzConnector.handleMessage(payload);
    }

    @Override
    public void handleTransportError(@NotNull StompSession session, @NotNull Throwable exception) {
        log.error("No connection to raspberry possible", exception);
        deconzConnector.connect();
    }

    @Override
    public void handleException(@NotNull StompSession session,
                                StompCommand command,
                                @NotNull StompHeaders headers,
                                @NotNull byte[] payload,
                                @NotNull Throwable exception) {
        log.error("Unexpected exception", exception);
        deconzConnector.connect();
    }
}
