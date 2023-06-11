package ch.akop.homesystem.services.impl;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import ch.akop.homesystem.persistence.repository.config.RollerShutterConfigRepository;
import ch.akop.homesystem.util.TimeUtil;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RollerShutterService {

  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final RollerShutterConfigRepository rollerShutterConfigRepository;
  private final TelegramMessageService telegramMessageService;

  private final List<Disposable> disposables = new ArrayList<>();
  private final Map<LocalTime, List<String>> timeToConfigs = new HashMap<>();


  @Transactional
  public void init() {
    initWeatherBasedRollerShutters();
    initTimer();
  }

  private void initWeatherBasedRollerShutters() {
    rollerShutterConfigRepository.findAll()
        .stream()
        .filter(config -> config.getCompassDirection() != null)
        .map(rollerShutterConfig -> weatherService.getCurrentAndPreviousWeather()
            .subscribe(weather -> {
              var currentComparedToPrevious = weather.current().getLight().compareTo(weather.previous().getLight().getAs(KILO_LUX));
              if (currentComparedToPrevious < 0) {
                itsGettingDarkerOutside(rollerShutterConfig, weather);
              } else if (currentComparedToPrevious > 0) {
                itsGettingBrighterOutside(rollerShutterConfig, weather);
              }
            }))
        .forEach(disposables::add);
  }

  private void initTimer() {
    rollerShutterConfigRepository.findAll().stream()
        .filter(config -> config.getCloseAt() != null || config.getOpenAt() != null)
        .forEach(config -> {
          Optional.ofNullable(config.getOpenAt())
              .map(localTime -> timeToConfigs.computeIfAbsent(localTime, ignored -> new ArrayList<>()))
              .ifPresent(list -> list.add(config.getName()));

          Optional.ofNullable(config.getCloseAt())
              .map(localTime -> timeToConfigs.computeIfAbsent(localTime, ignored -> new ArrayList<>()))
              .ifPresent(list -> list.add(config.getName()));
        });

    if (timeToConfigs.isEmpty()) {
      // no defined times = nop
      return;
    }

    disposables.add(Observable.defer(this::timerForNextEvent)
        .repeat()
        .subscribe());
  }

  private Observable<LocalTime> timerForNextEvent() {
    var nextExecutionTime = getNextExecutionTime().atZone(ZoneId.systemDefault());
    var nextEvent = Duration.between(ZonedDateTime.now(), nextExecutionTime);

    return Observable.timer(nextEvent.toSeconds(), TimeUnit.SECONDS)
        .map(ignored -> nextExecutionTime.toLocalTime())
        .doOnNext(this::handleTime);
  }

  private void handleTime(LocalTime time) {
    timeToConfigs.get(time)
        .stream()
        .map(id -> rollerShutterConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("RollerShutterConfig %s is not in database".formatted(id))))
        .forEach(config -> {
          var rollerShutter = getRollerShutter(config);
          if (config.getCloseAt() != null && config.getCloseAt().equals(time)) {
            log.info("Timed close for {}", rollerShutter.getName());
            rollerShutter.setLiftAndThenTilt(0, 0);
          } else {
            log.info("Timed open for {}", rollerShutter.getName());
            rollerShutter.setLiftAndThenTilt(100, 100);
          }
        });
  }

  private LocalDateTime getNextExecutionTime() {
    return timeToConfigs
        .keySet()
        .stream()
        .map(TimeUtil::getLocalDateTimeForTodayOrTomorrow)
        .min(LocalDateTime::compareTo)
        .orElseThrow();
  }

  @PreDestroy
  void tearDown() {
    disposables.forEach(Disposable::dispose);
  }


  private void itsGettingBrighterOutside(RollerShutterConfig config,
      WeatherService.CurrentAndPreviousWeather weather) {
    var rollerShutter = getRollerShutter(config);

    if (rollerShutter.isOpen()
        && shouldCloseBecauseOfSun(weather, config.getCompassDirection())
        && weather.current().getOuterTemperatur().isBiggerThan(15, DEGREE)) {
      log.info("Weather close for {} because it is too much sun", rollerShutter.getName());
      rollerShutter.setLiftAndThenTilt(70, 30);
    } else if (!rollerShutter.isOpen()
        && brightnessIsGoingAboveThreshold(weather, 50)
        && !config.isIgnoreWeatherInTheMorning()) {
      log.info("Weather open for {}", rollerShutter.getName());
      rollerShutter.open();
    }
  }

  private boolean shouldCloseBecauseOfSun(WeatherService.CurrentAndPreviousWeather weather,
      CompassDirection directionOfRollerShutter) {

    if (!brightnessIsGoingAboveThreshold(weather, 100)) {
      return false;
    }

    var sunDirection = weatherService.getCurrentSunDirection();
    return sunDirection.getZenithAngle() < 55
        && resolveCompassDirection(sunDirection).equals(directionOfRollerShutter);
  }

  private CompassDirection resolveCompassDirection(AzimuthZenithAngle sunDirection) {
    return Arrays.stream(CompassDirection.values())
        .min(Comparator.comparing(value -> Math.abs(value.getDirection() - sunDirection.getAzimuth())))
        .orElseThrow(() -> new NoSuchElementException("Can't resolve direction for %s".formatted(sunDirection)));
  }

  private void itsGettingDarkerOutside(RollerShutterConfig config,
      WeatherService.CurrentAndPreviousWeather weather) {
    var rollerShutter = getRollerShutter(config);

    if (rollerShutter.isOpen()
        && brightnessIsGoingBelowThreshold(weather, 0)
        && !config.isIgnoreWeatherInTheEvening()) {
      log.info("Weather close for {} because it is getting dark", rollerShutter.getName());
      rollerShutter.close();
    } else if (!rollerShutter.isOpen() && brightnessIsGoingBelowThreshold(weather, 150)) {
      log.info("Weather open for {} because it is not longer super-bright outside", rollerShutter.getName());
      rollerShutter.open();
    }
  }

  @SuppressWarnings("SameParameterValue")
  private boolean brightnessIsGoingAboveThreshold(WeatherService.CurrentAndPreviousWeather weather, int threshold) {
    var current = weather.current().getLight().getAs(KILO_LUX).intValue();
    var previous = weather.previous().getLight().getAs(KILO_LUX).intValue();

    return current >= threshold && previous < threshold;
  }

  private boolean brightnessIsGoingBelowThreshold(WeatherService.CurrentAndPreviousWeather weather, int threshold) {
    var current = weather.current().getLight().getAs(KILO_LUX).intValue();
    var previous = weather.previous().getLight().getAs(KILO_LUX).intValue();

    return current <= threshold && previous > threshold;
  }

  private RollerShutter getRollerShutter(RollerShutterConfig config) {
    return deviceService.findDeviceByName(config.getName(), RollerShutter.class)
        .orElseThrow(() -> new NoSuchElementException("No rollerShutter named '%s' was found in deviceList.".formatted(config.getName())));
  }
}
