package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.scraper.weathercloud.Scraper;
import ch.akop.weathercloud.temperature.Temperature;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;

import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;
import static ch.akop.weathercloud.wind.WindSpeedUnit.KILOMETERS_PER_SECOND;
import static java.time.temporal.ChronoUnit.MINUTES;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RequiredArgsConstructor
@Service
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final HomeConfig config;
    private final MessageService messageService;

    @Getter
    private final Subject<Weather> weather = ReplaySubject.create(1);

    @Getter
    private boolean active;

    private Temperature lastMeasuredTemperature;

    @PostConstruct
    public void startFetchingData() {
        if (this.config.getNearestWeatherCloudStation() == null) {
            this.active = false;
            return;
        }

        this.active = true;
        log.info("WeatherService will be started.");
        new Scraper()
                .scrape$(this.config.getNearestWeatherCloudStation(), Duration.of(5, MINUTES))
                .subscribe(this.weather::onNext);

        this.weather.subscribe(weather -> {
            if (weather.getWind().getAs(KILOMETERS_PER_SECOND).compareTo(new BigDecimal(50)) > 0) {
                this.messageService.sendMessageToUser("Hui, ist das winding. Macht lieber die Stören hoch. Grade weht mit %s."
                        .formatted(weather.getWind()));
            }

            if (this.lastMeasuredTemperature == null) {
                this.lastMeasuredTemperature = weather.getOuterTemperatur();
            } else if (this.lastMeasuredTemperature.getAs(DEGREE)
                    .subtract(weather.getOuterTemperatur().getAs(DEGREE))
                    .abs()
                    .compareTo(new BigDecimal(15)) > 0) {
                this.messageService.sendMessageToUser("Draussen sind grade so etwa %s.".formatted(weather.getOuterTemperatur()));
                this.lastMeasuredTemperature = weather.getOuterTemperatur();
            }

        });
    }
}
