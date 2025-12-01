package ch.akop.homesystem.services.impl;

import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static java.math.BigDecimal.ZERO;

import ch.akop.homesystem.persistence.model.RainStats;
import ch.akop.homesystem.persistence.repository.RainStatsRepository;
import ch.akop.weathercloud.Weather;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@ApplicationScoped
@Startup
public class RainDetectorService {

  private final RainStatsRepository rainStatsRepository;


  @Transactional
  public void updateDatabaseIfNecessary(final Weather weather) {
    final var lastState = Optional.ofNullable(this.rainStatsRepository.findFirstByOrderByMeasuredAtDesc())
        .map(RainStats::isRaining)
        .orElse(false);

    final var currentState = weather.getRain().isBiggerThan(ZERO, MILLIMETER_PER_HOUR);
    if (currentState != lastState) {
      this.rainStatsRepository.save(new RainStats().setRaining(currentState));
    }
  }


  @Transactional
  public Duration noRainFor() {
    final var lastRainDate = Optional.ofNullable(this.rainStatsRepository.findFirstByOrderByMeasuredAtDesc())
        .filter(rainStats -> !rainStats.isRaining())
        .map(RainStats::getMeasuredAt)
        .orElse(LocalDateTime.now());

    return Duration.between(lastRainDate, LocalDateTime.now());
  }
}
