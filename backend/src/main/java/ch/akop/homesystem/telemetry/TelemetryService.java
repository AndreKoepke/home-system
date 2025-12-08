package ch.akop.homesystem.telemetry;


import ch.akop.homesystem.external.akop.Heartbeat;
import ch.akop.homesystem.external.akop.TelemetryServer;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class TelemetryService {

  private final TelemetryRepository telemetryRepository;


  private TelemetryServer telemetryServer;

  @PostConstruct
  @SneakyThrows
  public void initialize() {
    telemetryServer = RestClientBuilder.newBuilder()
        .baseUri(URI.create("https://home-system-telemetry.akop.online/"))
        .build(TelemetryServer.class);

    if (telemetryRepository.findAll().isEmpty()) {
      telemetryRepository.save(new TelemetryData().setId(telemetryServer.sync().getId()));
    }
  }


  @Scheduled(every = "24h")
  public void doHeartbeat() {
    telemetryRepository.findAll()
        .forEach(telemetryData -> telemetryServer.heartbeat(new Heartbeat()
            .setId(telemetryData.getId())
            .setVersion(ConfigProvider.getConfig().getValue("quarkus.application.version", String.class))));
  }
}
