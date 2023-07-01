package ch.akop.homesystem.telemetry;


import ch.akop.homesystem.external.akop.Heartbeat;
import ch.akop.homesystem.external.akop.TelemetryServer;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class TelemetryService {

  private final TelemetryRepository telemetryRepository;


  private TelemetryServer telemetryServer;
  private String gitBranch;
  private String gitCommit;
  private LocalDateTime gitCommitDate;

  @PostConstruct
  @SneakyThrows
  public void initialize() {
    telemetryServer = RestClientBuilder.newBuilder()
        .baseUri(URI.create("https://home-system-telemetry.akop.online/"))
        .build(TelemetryServer.class);

    var prop = new Properties();
    prop.load(TelemetryService.class.getClassLoader().getResourceAsStream("git.properties"));
    gitBranch = prop.getProperty("git.branch");
    gitCommit = prop.getProperty("git.commit.id.abbrev");
    gitCommitDate = LocalDateTime.parse(prop.getProperty("git.commit.time"));

    if (telemetryRepository.findAll().isEmpty()) {
      telemetryRepository.save(new TelemetryData().setId(telemetryServer.sync().getId()));
    }
  }


  @Scheduled(every = "24h")
  public void doHeartbeat() {
    telemetryRepository.findAll()
        .forEach(telemetryData -> telemetryServer.heartbeat(new Heartbeat()
            .setId(telemetryData.getId())
            .setGitBranch("")
            .setGitCommit("")
            .setGitCommitDate(LocalDateTime.now())));
  }
}
