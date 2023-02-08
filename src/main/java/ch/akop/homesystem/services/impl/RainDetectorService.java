package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.persistence.model.RainStats;
import ch.akop.homesystem.persistence.repository.RainStatsRepository;
import ch.akop.weathercloud.Weather;
import io.quarkus.runtime.Startup;
import lombok.RequiredArgsConstructor;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;

import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static java.math.BigDecimal.ZERO;

@RequiredArgsConstructor
@ApplicationScoped
@Startup
public class RainDetectorService {

    private final RainStatsRepository rainStatsRepository;


    @Transactional
    public void updateDatabaseIfNecessary(final Weather weather) {
        final var lastState = this.rainStatsRepository.findFirstByOrderByMeasuredAtDesc()
                .map(RainStats::isRaining)
                .orElse(false);

        final var currentState = weather.getRain().isBiggerThan(ZERO, MILLIMETER_PER_HOUR);
        if (currentState != lastState) {
            this.rainStatsRepository.save(new RainStats().setRaining(currentState));
        }
    }


    @Transactional
    public Duration noRainFor() {
        final var lastRainDate = this.rainStatsRepository.findFirstByOrderByMeasuredAtDesc()
                .filter(rainStats -> !rainStats.isRaining())
                .map(RainStats::getMeasuredAt)
                .orElse(LocalDateTime.now());

        return Duration.between(lastRainDate, LocalDateTime.now());
    }
}
