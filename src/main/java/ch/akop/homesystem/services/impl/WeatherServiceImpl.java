package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.scraper.weathercloud.Scraper;
import com.jakewharton.rx3.ReplayingShare;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.Grena3;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import static ch.akop.weathercloud.wind.WindSpeedUnit.KILOMETERS_PER_SECOND;
import static java.time.temporal.ChronoUnit.MINUTES;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RequiredArgsConstructor
@Service
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final BasicConfigRepository basicConfigRepository;
    private final MessageService messageService;

    @Getter
    private Flowable<Weather> weather;

    @Getter
    private boolean active;

    @PostConstruct
    public void startFetchingData() {
        // TODO restart when config changes
        var config = basicConfigRepository.findFirstByOrderByModifiedDesc();
        if (config.getNearestWeatherCloudStation() == null) {
            active = false;
            return;
        }

        active = true;
        log.info("WeatherService will be started.");
        weather = new Scraper()
                .scrape$(config.getNearestWeatherCloudStation(), Duration.of(5, MINUTES))
                .compose(ReplayingShare.instance());

        weather.subscribe(weatherUpdate -> {
            if (weatherUpdate.getWind().getAs(KILOMETERS_PER_SECOND).compareTo(new BigDecimal(50)) > 0) {
                messageService.sendMessageToMainChannel("Hui, ist das winding. Macht lieber die Stören hoch. Grade wehts mit %s."
                        .formatted(weatherUpdate.getWind()));
            }
        });
    }


    @Override
    public Flowable<CurrentAndPreviousWeather> getCurrentAndPreviousWeather() {
        var previousUpdate = new AtomicReference<Weather>();

        weather
                .take(1)
                .subscribe(previousUpdate::set);

        return weather
                .skip(1)
                .map(weatherUpdate -> new CurrentAndPreviousWeather(weatherUpdate, previousUpdate.get()))
                .doOnNext(weatherData -> previousUpdate.set(weatherData.current));
    }

    @Override
    public CompassDirection getCurrentSunDirection() {
        var config = basicConfigRepository.findFirstByOrderByModifiedDesc();
        var position = Grena3.calculateSolarPosition(new GregorianCalendar(), config.getLatitude(), config.getLongitude(), 68);

        return Arrays.stream(CompassDirection.values())
                .min(Comparator.comparing(value -> Math.abs(value.getDirection() - position.getAzimuth())))
                .orElseThrow(() -> new NoSuchElementException("Can't resolve direction for %s".formatted(position)));
    }

    public record CurrentAndPreviousWeather(Weather current, Weather previous) {
    }
}
