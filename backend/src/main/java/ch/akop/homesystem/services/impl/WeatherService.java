package ch.akop.homesystem.services.impl;

import static java.time.temporal.ChronoUnit.MINUTES;

import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.light.LightUnit;
import ch.akop.weathercloud.scraper.weathercloud.Scraper;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.Grena3;
import net.e175.klaus.solarpositioning.SolarPosition;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RequiredArgsConstructor
@ApplicationScoped
@Slf4j
public class WeatherService {

  private final BasicConfigRepository basicConfigRepository;
  private final RainDetectorService rainDetectorService;

  @Getter
  private final ReplaySubject<Weather> weather = ReplaySubject.createWithSize(1);

  @Getter
  private boolean active;

  private LocalDateTime gotDarkAt = LocalDateTime.MIN;


  @PostConstruct
  void startFetchingData() {
    // TODO restart when config changes
    var nearestWeatherCloudStation = basicConfigRepository.findByOrderByModifiedDesc()
        .map(BasicConfig::getNearestWeatherCloudStation)
        .orElse(null);

    if (nearestWeatherCloudStation == null) {
      log.warn("No weather station, no weather updates");
      return;
    }

    active = true;
    new Scraper()
        .scrape$(nearestWeatherCloudStation, Duration.of(5, MINUTES))
        .doOnError(throwable -> log.error("There was an error while scraping data from weather cloud", throwable))
        .retryWhen(errors$ ->
            errors$
                .scan(0, (numberOfTotalErrors, lastError) -> numberOfTotalErrors + 1)
                .map(numberOfTotalErrors -> Observable.timer((long) Math.min(Math.pow(4, numberOfTotalErrors), 180), TimeUnit.SECONDS))
        )
        .subscribe(weather::onNext);

    weather.subscribe(weatherUpdate -> {
      log.info("Got weather-update " + weatherUpdate);
      rainDetectorService.updateDatabaseIfNecessary(weatherUpdate);
    });

    getCurrentAndPreviousWeather().subscribe(this::checkDarkness);

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

  /**
   * @return How long it is already dark today. Returns PT0 when it's bright outside.
   */
  public Duration outSideDarkFor() {
    if (gotDarkAt == null) {
      return Duration.ZERO;
    }

    return Duration.between(LocalDateTime.now(), gotDarkAt).abs();
  }

  @Transactional
  public SolarPosition getCurrentSunDirection() {
    return basicConfigRepository.findByOrderByModifiedDesc()
        .map(config -> {

          if (config.getLatitude() == null || config.getLongitude() == null) {
            log.warn("Latitude or longitude is not set");
            return null;
          }

          return Grena3.calculateSolarPosition(ZonedDateTime.now(), config.getLatitude(), config.getLongitude(), 68);
        })
        .orElseThrow(() -> new RuntimeException("No basic config"));
  }

  public void checkDarkness(CurrentAndPreviousWeather update) {
    var currentLight = update.current().getLight().getAs(LightUnit.KILO_LUX).intValue();
    var previousLight = update.previous().getLight().getAs(LightUnit.KILO_LUX).intValue();

    if (currentLight > 0) {
      gotDarkAt = null;
    } else if (previousLight > 0) {
      gotDarkAt = LocalDateTime.now();
    }
  }

  public record CurrentAndPreviousWeather(Weather current, Weather previous) {

  }
}
