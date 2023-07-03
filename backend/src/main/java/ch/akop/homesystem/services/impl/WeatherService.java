package ch.akop.homesystem.services.impl;

import static java.time.temporal.ChronoUnit.MINUTES;

import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.scraper.weathercloud.Scraper;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.Grena3;

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
        .subscribe(weather::onNext);

    weather.subscribe(weatherUpdate -> {
      log.info("Got weather-update " + weatherUpdate);
      rainDetectorService.updateDatabaseIfNecessary(weatherUpdate);
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

  @Transactional
  public AzimuthZenithAngle getCurrentSunDirection() {
    return basicConfigRepository.findFirstByOrderByModifiedDesc()
        .map(config -> Grena3.calculateSolarPosition(ZonedDateTime.now(), config.getLatitude(), config.getLongitude(), 68))
        .orElseThrow(() -> new RuntimeException("No basic config"));
  }

  public record CurrentAndPreviousWeather(Weather current, Weather previous) {

  }
}
