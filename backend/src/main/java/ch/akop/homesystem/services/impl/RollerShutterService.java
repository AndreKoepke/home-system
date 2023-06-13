package ch.akop.homesystem.services.impl;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import ch.akop.homesystem.persistence.repository.config.RollerShutterConfigRepository;
import ch.akop.homesystem.util.TimeUtil;
import ch.akop.weathercloud.Weather;
import io.quarkus.runtime.StartupEvent;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
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
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import org.eclipse.microprofile.context.ManagedExecutor;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RollerShutterService {

  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final RollerShutterConfigRepository rollerShutterConfigRepository;
  private final TelegramMessageService telegramMessageService;
  private final ManagedExecutor executor;
  private final Vertx vertx;
  private Scheduler rxScheduler;

  void onStart(@Observes StartupEvent ev) {
    rxScheduler = RxHelper.blockingScheduler(vertx);
  }

  private final List<Disposable> disposables = new ArrayList<>();
  private final Map<LocalTime, List<String>> timeToConfigs = new HashMap<>();


  @Transactional
  public void init() {
    disposables.add(weatherService.getWeather()
        .skip(1)
        .mergeWith(telegramMessageService.getMessages()
            .filter(message -> message.startsWith("/calcRollerShutter"))
            .switchMap(message -> weatherService.getWeather().take(1)))
        .subscribeOn(rxScheduler)
        .subscribe(newWeather -> executor.runAsync(() -> handleWeatherUpdate(newWeather))));
    initTimer();
  }

  private void handleWeatherUpdate(Weather newWeather) {
    var newBrightness = newWeather.getLight().getAs(KILO_LUX).intValue();

    if (newBrightness > 150) {
      var sunDirection = weatherService.getCurrentSunDirection();
      var compassDirection = resolveCompassDirection(sunDirection);

      if (newWeather.getOuterTemperatur().isSmallerThan(15, DEGREE) && sunDirection.getZenithAngle() > 55) {
        return;
      }

      rollerShutterConfigRepository.findByCompassDirection(compassDirection)
          .map(this::getRollerShutter)
          .filter(rollerShutter -> rollerShutter.getCurrentLift() > 45)
          .peek(rollerShutter -> log.info("Weather close for " + rollerShutter.getName() + " because it is too much sun"))
          .forEach(rollerShutter -> rollerShutter.setLiftAndThenTilt(50, 50));

    } else if (newBrightness < 100 && newBrightness > 10) {
      rollerShutterConfigRepository.findRollerShutterConfigByCompassDirectionIsNotNull()
          .map(this::getRollerShutter)
          .filter(rollerShutter -> rollerShutter.getCurrentLift() < 60 && rollerShutter.getCurrentLift() > 40)
          .peek(rollerShutter -> log.info("Open RollerShutter " + rollerShutter.getName() + ". It is not so bright anymore"))
          .forEach(RollerShutter::open);
    } else if (newBrightness == 0) {
      rollerShutterConfigRepository.findRollerShutterConfigByCompassDirectionIsNotNull()
          .map(this::getRollerShutter)
          .filter(RollerShutter::isNotCompletelyClosed)
          .peek(rollerShutter -> log.info("Close RollerShutter " + rollerShutter.getName() + " because it is night."))
          .forEach(RollerShutter::close);
    }
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
