package ch.akop.homesystem.controller.for_private.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.Session;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractBaseSocket {

  abstract ObjectMapper getObjectMapper();

  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final Map<String, Set<Integer>> sendHashCodes = new ConcurrentHashMap<>();

  public void registerSession(Session session) {
    sessions.put(session.getId(), session);
  }

  @SneakyThrows
  public void deregisterSession(String sessionId) {
    sessions.remove(sessionId);
    sendHashCodes.remove(sessionId);
  }

  public void broadcast(Object message) {
    sessions.values().forEach(session -> sendMessage(session, message));
  }

  @SneakyThrows
  public void sendMessage(String sessionId, Object message) {
    var session = sessions.get(sessionId);

    sendHashCodes.putIfAbsent(sessionId, new HashSet<>());
    if (sendHashCodes.get(sessionId).contains(message.hashCode())) {
      return;
    }
    sendHashCodes.getOrDefault(sessionId, new HashSet<>()).add(message.hashCode());

    var payload = getObjectMapper().writeValueAsString(message);
    session.getAsyncRemote().sendObject(payload, result -> {
      if (result.getException() != null) {
        log.error("Error while sending message", result.getException());
      }
    });
  }
}
