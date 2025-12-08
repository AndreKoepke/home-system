package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.Identable;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBaseSocket {

  abstract ObjectMapper getObjectMapper();

  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Integer>> sendHashCodes = new ConcurrentHashMap<>();

  public void registerSession(Session session) {
    sessions.put(session.getId(), session);
  }

  @SneakyThrows
  public void deregisterSession(String sessionId) {
    sessions.remove(sessionId);
    sendHashCodes.remove(sessionId);
  }

  public void broadcast(Identable message) {
    sessions.keySet().forEach(sessionId -> sendMessage(sessionId, message));
  }

  @SneakyThrows
  public void sendMessage(String sessionId, Identable message) {
    var session = sessions.get(sessionId);

    sendHashCodes.putIfAbsent(sessionId, new HashMap<>());
    sendHashCodes.get(sessionId).putIfAbsent(message.getId(), 0);
    if (sendHashCodes.get(sessionId).get(message.getId()).equals(message.hashCode())) {
      return;
    }
    sendHashCodes.get(sessionId).put(message.getId(), message.hashCode());

    var payload = getObjectMapper().writeValueAsString(message);
    session.getAsyncRemote().sendObject(payload, result -> {
      if (result.getException() != null) {
        log.error("Error while sending message", result.getException());
      }
    });
  }
}
