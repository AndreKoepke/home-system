package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.controller.dtos.WeatherDto;
import ch.akop.homesystem.services.impl.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
@ServerEndpoint("/secured/ws/v1/weather")
public class WeatherChangedSocket {

  private final WeatherService weatherService;
  private final ObjectMapper objectMapper;

  private String currentWeatherJsonPayload;

  Map<String, Session> sessions = new ConcurrentHashMap<>();

  @PostConstruct
  void listenForWeatherChanges() {
    weatherService.getWeather()
        .map(WeatherDto::from)
        .map(objectMapper::writeValueAsString)
        .subscribe(weather -> {
          currentWeatherJsonPayload = weather;
          broadcastWeather();
        });
  }

  @OnOpen
  public void onOpen(Session session) {
    log.info("Opening session: {} @ {}", session.getId());
    sessions.put(session.getId(), session);
    sendWeahterToSession(session.getAsyncRemote());
  }

  @OnClose
  public void onClose(Session session) {
    log.info("Close session: {}", session.getId());
    sessions.remove(session.getId());
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    log.error("Error on session: {}", session.getId(), throwable);
    sessions.remove(session.getId());
  }

  @SneakyThrows
  private void broadcastWeather() {
    sessions.values()
        .stream()
        .map(Session::getAsyncRemote)
        .forEach(this::sendWeahterToSession);
  }

  private void sendWeahterToSession(RemoteEndpoint.Async target) {
    if (currentWeatherJsonPayload != null) {
      target.sendText(currentWeatherJsonPayload, result -> {
        if (result.getException() != null) {
          log.error("Error while sending weather", result.getException());
        }
      });
    }
  }
}
