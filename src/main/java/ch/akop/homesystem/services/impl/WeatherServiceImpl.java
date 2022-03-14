package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.scraper.weathercloud.Scraper;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;

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
    private final Subject<Weather> weather = ReplaySubject.createWithSize(1);

    @Getter
    private boolean active;

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
        });
    }
}
