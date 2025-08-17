package ch.akop.homesystem.authentication;

import ch.akop.homesystem.services.impl.TelegramMessageService;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class AuthenticationService {

  private final AuthenticationRepository authenticationRepository;
  private final TelegramMessageService telegramMessageService;

  @ConfigProperty(name = "BASE_URL")
  Optional<String> baseUrl;

  @PostConstruct
  @SneakyThrows
  public void init() {
    var host = baseUrl.orElseGet(this::getHost);

    telegramMessageService.waitForMessageOnce("registerNewWebUrl")
        .repeat()
        .map(registration -> authenticationRepository.save(new AuthenticationToken()
            .setId(UUID.randomUUID())
            .setToken(UUID.randomUUID().toString())))
        .subscribe(savedAuth -> telegramMessageService
            .sendMessageToMainChannel("Ok, der neue Token wurde erstellt. Du kannst jetzt mit folgender Url verwenden: "
                + host + "?api-key=" + savedAuth.getToken()));
  }

  public boolean isAuthenticated(String token) {
    return authenticationRepository.findByToken(token)
        .map(foundToken -> {
          foundToken.setLastTimeUsed(LocalDateTime.now());
          authenticationRepository.save(foundToken);
          return foundToken;
        })
        .isPresent();
  }

  @SneakyThrows
  private String getHost() {
    try (var socket = new Socket()) {
      socket.connect(new InetSocketAddress("1.1.1.1", 80));
      return "http:/" + socket.getLocalAddress() + ":8080";
    }
  }

  @ServerRequestFilter
  public Optional<RestResponse<Void>> filter(ContainerRequestContext ctx) {
    if (ctx.getUriInfo().getPath().startsWith("/secured") && !isAuthenticated(ctx.getHeaderString("api-key"))) {
      return Optional.of(RestResponse.status(Status.FORBIDDEN));
    }
    return Optional.empty();
  }
}
