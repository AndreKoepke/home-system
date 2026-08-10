package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.authentication.AuthenticationService;
import ch.akop.homesystem.controller.dtos.Identable;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBaseSocket {

  abstract ObjectMapper getObjectMapper();

  abstract AuthenticationService getAuthenticationService();

  private final Map<String, WebSocketConnection> connections = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Integer>> sendHashCodes = new ConcurrentHashMap<>();

  private record ControlMessage(String token) {

  }

  @SneakyThrows
  public boolean registerSession(WebSocketConnection connection, String loginMessageRaw) {
    var loginMessage = getObjectMapper().readValue(loginMessageRaw, ControlMessage.class);
    if (getAuthenticationService().isAuthenticated(loginMessage.token)) {
      connections.put(connection.id(), connection);
      return true;
    } else {
      connection.closeAndAwait(CloseReason.NORMAL);
    }

    return false;
  }

  @SneakyThrows
  public void deregisterSession(String sessionId) {
    connections.remove(sessionId);
    sendHashCodes.remove(sessionId);
  }

  public void broadcast(Identable message) {
    connections.keySet().forEach(sessionId -> sendMessage(sessionId, message));
  }

  @SneakyThrows
  public void sendMessage(String sessionId, Identable message) {
    var session = connections.get(sessionId);

    sendHashCodes.putIfAbsent(sessionId, new HashMap<>());
    sendHashCodes.get(sessionId).putIfAbsent(message.getId(), 0);
    if (sendHashCodes.get(sessionId).get(message.getId()).equals(message.hashCode())) {
      return;
    }
    sendHashCodes.get(sessionId).put(message.getId(), message.hashCode());

    var payload = getObjectMapper().writeValueAsString(message);
    session.sendTextAndAwait(payload);
  }
}
