package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.scraper.weathercloud.Scraper;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.Grena3;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import static ch.akop.weathercloud.wind.WindSpeedUnit.KILOMETERS_PER_SECOND;
import static java.time.temporal.ChronoUnit.MINUTES;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RequiredArgsConstructor
@ApplicationScoped
@Slf4j
public class WeatherService {

    private final BasicConfigRepository basicConfigRepository;
    private final TelegramMessageService messageService;
    private final RainDetectorService rainDetectorService;

    @Getter
    private final ReplaySubject<Weather> weather = ReplaySubject.createWithSize(1);

    @Getter
    private boolean active;

    @PostConstruct
    void startFetchingData() {
        // TODO restart when config changes
        var nearestWeatherCloudStation = basicConfigRepository.findFirstByOrderByModifiedDesc()
                .map(BasicConfig::getNearestWeatherCloudStation)
                .orElse(null);

        if (nearestWeatherCloudStation == null) {
            return;
        }

        active = true;
        new Scraper()
                .scrape$(nearestWeatherCloudStation, Duration.of(5, MINUTES))
                .subscribe(t -> {
                    weather.onNext(t);
                    weather.onNext(t);
                });

        weather.subscribe(weatherUpdate -> {
            rainDetectorService.updateDatabaseIfNecessary(weatherUpdate);
            if (weatherUpdate.getWind().getAs(KILOMETERS_PER_SECOND).compareTo(new BigDecimal(50)) > 0) {
                messageService.sendMessageToMainChannel("Hui, ist das winding. Macht lieber die Stören hoch. Grade wehts mit %s."
                        .formatted(weatherUpdate.getWind()));
            }
        });


        log.info("WeatherService is up");
    }


    public Observable<CurrentAndPreviousWeather> getCurrentAndPreviousWeather() {
        var previousUpdate = new AtomicReference<Weather>();

        weather
                .take(1)
                .subscribe(previousUpdate::set);

        return weather
                .skip(1)
                .map(weatherUpdate -> new CurrentAndPreviousWeather(weatherUpdate, previousUpdate.get()))
                .doOnNext(weatherData -> previousUpdate.set(weatherData.current));
    }

    public CompassDirection getCurrentSunDirection() {
        var config = basicConfigRepository.findFirstByOrderByModifiedDesc()
                .orElseThrow();
        var position = Grena3.calculateSolarPosition(ZonedDateTime.now(), config.getLatitude(), config.getLongitude(), 68);

        return Arrays.stream(CompassDirection.values())
                .min(Comparator.comparing(value -> Math.abs(value.getDirection() - position.getAzimuth())))
                .orElseThrow(() -> new NoSuchElementException("Can't resolve direction for %s".formatted(position)));
    }

    public record CurrentAndPreviousWeather(Weather current, Weather previous) {
    }
}
