package ch.akop.homesystem.controller.for_private.websocket;

import ch.akop.homesystem.authentication.AuthenticationService;
import ch.akop.homesystem.controller.dtos.WeatherDto;
import ch.akop.homesystem.services.impl.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
@WebSocket(path = "/secured/ws/v1/weather")
public class WeatherChangedSocket extends AbstractBaseSocket {

  private final WeatherService weatherService;

  @Getter
  private final ObjectMapper objectMapper;

  @Getter
  private final AuthenticationService authenticationService;

  private WeatherDto currentWeather;


  @PostConstruct
  void listenForWeatherChanges() {
    weatherService.getWeather()
        .map(WeatherDto::from)
        .subscribe(weather -> {
          currentWeather = weather;
          broadcastWeather();
        });
  }

  @OnOpen
  public void onOpen(WebSocketConnection session) {

  }

  @OnTextMessage
  public void onMessage(String message, WebSocketConnection session) {
    if (registerSession(session, message)) {
      sendMessage(session.id(), currentWeather);
    }
  }

  @OnClose
  public void onClose(WebSocketConnection session) {
    deregisterSession(session.id());
  }

  @OnError
  public void onError(WebSocketConnection session, Throwable throwable) {
    log.error("Error on session: {}", session.id(), throwable);
    deregisterSession(session.id());
  }

  @SneakyThrows
  private void broadcastWeather() {
    broadcast(currentWeather);
  }

}
