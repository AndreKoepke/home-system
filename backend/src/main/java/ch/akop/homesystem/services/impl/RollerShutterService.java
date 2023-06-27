package ch.akop.homesystem.services.impl;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import ch.akop.homesystem.persistence.repository.config.RollerShutterConfigRepository;
import ch.akop.homesystem.util.TimeUtil;
import ch.akop.weathercloud.Weather;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
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
import java.util.function.Function;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import org.jetbrains.annotations.NotNull;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RollerShutterService {

  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final RollerShutterConfigRepository rollerShutterConfigRepository;
  private final TelegramMessageService telegramMessageService;
  private final Vertx vertx;

  private final List<Disposable> disposables = new ArrayList<>();
  private final Map<LocalTime, List<String>> timeToConfigs = new HashMap<>();


  public void init() {
    var rxScheduler = RxHelper.blockingScheduler(vertx);
    disposables.add(weatherService.getWeather()
        .mergeWith(telegramMessageService.getMessages()
            .filter(message -> message.startsWith("/calcRollerShutter"))
            .switchMap(message -> weatherService.getWeather().take(1)))
        .debounce(10, TimeUnit.SECONDS)
        .subscribeOn(rxScheduler)
        .flatMapCompletable(weather -> Completable.merge(handleWeatherUpdate(weather)))
        .subscribe());

    initTimer();
  }

  private List<Completable> handleWeatherUpdate(Weather newWeather) {
    var configs = QuarkusTransaction.requiringNew().call(() -> rollerShutterConfigRepository.findRollerShutterConfigByCompassDirectionIsNotNull().toList());
    var newBrightness = newWeather.getLight().getAs(KILO_LUX).intValue();

    if (newBrightness > 250) {
      if (newWeather.getOuterTemperatur().isSmallerThan(15, DEGREE)) {
        return new ArrayList<>();
      }

      var sunDirection = QuarkusTransaction.requiringNew().call(weatherService::getCurrentSunDirection);
      var compassDirection = resolveCompassDirection(sunDirection);

      return configs.stream()
          .map(handleHighBrightness(sunDirection, compassDirection))
          .toList();

    } else if (newBrightness < 70 && newBrightness > 10) {
      return configs.stream()
          .filter(rollerShutterConfig -> !rollerShutterConfig.isIgnoreWeatherInTheMorning())
          .map(this::getRollerShutter)
          .peek(rollerShutter -> log.info("Open RollerShutter " + rollerShutter.getName() + "."))
          .map(RollerShutter::open)
          .toList();
    } else if (newBrightness == 0) {
      return configs.stream()
          .filter(rollerShutterConfig -> !rollerShutterConfig.isIgnoreWeatherInTheEvening())
          .map(this::getRollerShutter)
          .peek(rollerShutter -> log.info("Close RollerShutter " + rollerShutter.getName() + " because it is night."))
          .map(RollerShutter::close)
          .toList();
    }

    return new ArrayList<>();
  }

  @NotNull
  private Function<RollerShutterConfig, Completable> handleHighBrightness(AzimuthZenithAngle sunDirection, CompassDirection compassDirection) {
    return config -> {
      var rollerShutter = getRollerShutter(config);

      if (config.getCompassDirection().contains(compassDirection)) {
        log.info("Weather close for " + rollerShutter.getName()
            + " because it is too much sun. Zenith is "
            + sunDirection.getZenithAngle() + "°.");

        if (sunDirection.getZenithAngle() > 40 && rollerShutter.getCurrentLift() > 50) {
          return rollerShutter.setLiftAndThenTilt(50, 50);
        } else if (sunDirection.getZenithAngle() > 20 && rollerShutter.getCurrentLift() > 75) {
          return rollerShutter.setLiftAndThenTilt(75, 75);
        }
      }

      if (config.isIgnoreWeatherInTheMorning()) {
        return Completable.complete();
      }

      log.info("Weather open for " + rollerShutter.getName() + " because sun is coming from other direction");
      return rollerShutter.open();
    };
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
            rollerShutter.setLiftAndThenTilt(0, 0).subscribe();
          } else {
            log.info("Timed open for {}", rollerShutter.getName());
            rollerShutter.setLiftAndThenTilt(100, 100).subscribe();
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

  private CompassDirection resolveCompassDirection(AzimuthZenithAngle sunDirection) {
    return Arrays.stream(CompassDirection.values())
        .min(Comparator.comparing(value -> Math.abs(value.getDirection() - sunDirection.getAzimuth())))
        .orElseThrow(() -> new NoSuchElementException("Can't resolve direction for %s".formatted(sunDirection)));
  }

  private RollerShutter getRollerShutter(RollerShutterConfig config) {
    return deviceService.findDeviceByName(config.getName(), RollerShutter.class)
        .orElseThrow(() -> new NoSuchElementException("No rollerShutter named '%s' was found in deviceList.".formatted(config.getName())));
  }
}
