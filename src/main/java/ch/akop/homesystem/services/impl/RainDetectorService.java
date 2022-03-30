package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.persistence.model.RainStats;
import ch.akop.homesystem.persistence.repository.RainStatsRepository;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;

import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static java.math.BigDecimal.ZERO;

@RequiredArgsConstructor
@Service
public class RainDetectorService {

    private final RainStatsRepository rainStatsRepository;
    private final WeatherService weatherService;

    @PostConstruct
    public void monitorWeatherData() {
        //noinspection ResultOfMethodCallIgnored
        this.weatherService.getWeather().subscribe(this::updateDatabaseIfNecessary);
    }

    private void updateDatabaseIfNecessary(final Weather weather) {
        final var lastState = this.rainStatsRepository.findFirstByOrderByMeasuredAtDesc()
                .map(RainStats::isRaining)
                .orElse(false);

        final var currentState = weather.getRain().isBiggerThan(ZERO, MILLIMETER_PER_HOUR);
        if (currentState != lastState) {
            this.rainStatsRepository.save(new RainStats().setRaining(currentState));
        }
    }


    public Duration noRainFor() {
        final var lastRainDate = this.rainStatsRepository.findFirstByOrderByMeasuredAtDesc()
                .filter(rainStats -> !rainStats.isRaining())
                .map(RainStats::getMeasuredAt)
                .orElse(LocalDateTime.now());

        return Duration.between(lastRainDate, LocalDateTime.now());
    }
}
