package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.util.Comparer.is;
import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;
import static ch.akop.weathercloud.wind.WindSpeedUnit.METERS_PER_SECOND;
import static java.util.concurrent.TimeUnit.SECONDS;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import ch.akop.homesystem.persistence.repository.config.RollerShutterConfigRepository;
import ch.akop.homesystem.util.TimeUtil;
import ch.akop.homesystem.util.TimedGateKeeper;
import ch.akop.weathercloud.Weather;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.text.DateFormat;
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
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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

  public static final Duration TIMEOUT_AFTER_MANUAL = Duration.ofHours(1);
  private static final DateFormat GERMANY_DATE_TIME = DateFormat.getDateTimeInstance(
      DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY);

  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final RollerShutterConfigRepository rollerShutterConfigRepository;
  private final TelegramMessageService telegramMessageService;
  private final Vertx vertx;

  private final List<Disposable> disposables = new ArrayList<>();
  private final Map<LocalTime, List<String>> timeToConfigs = new HashMap<>();
  private final TimedGateKeeper highSunLock = new TimedGateKeeper();

  public void init() {
    var rxScheduler = RxHelper.blockingScheduler(vertx);
    disposables.add(weatherService.getWeather()
        .doOnNext(this::checkWindSpeed)
        .mergeWith(telegramMessageService.getMessages()
            .filter(message -> message.startsWith("/calcRollerShutter"))
            .switchMap(message -> weatherService.getWeather().take(1)))
        .debounce(10, SECONDS)
        .subscribeOn(rxScheduler)
        .flatMapCompletable(weather -> Completable.merge(handleWeatherUpdate(weather)))
        .subscribe());

    disposables.add(telegramMessageService.getMessages()
        .filter(message -> message.startsWith("/noAutomaticsForRollerShutter"))
        .subscribeOn(rxScheduler)
        .doOnNext(message -> telegramMessageService.sendMessageToMainChannel("Ok, welche Störe soll ich eine Zeit in Ruhe lassen?"))
        .doOnNext(message -> deviceService.getDevicesOfType(RollerShutter.class)
            .forEach(rollerShutter -> telegramMessageService.sendMessageToMainChannel(rollerShutter.getName())))
        .switchMap(ignoredMessage -> telegramMessageService.getMessages()
            .map(messageTarget -> deviceService.findDeviceByName(messageTarget.substring(1), RollerShutter.class))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(rollerShutter -> rollerShutterConfigRepository.findByNameLike(rollerShutter.getName())
                .orElseThrow(() -> new RuntimeException("Not found")))
            .take(1)
            .doOnNext(rollerShutter -> telegramMessageService.sendMessageToMainChannel("Aye. Für wie lange?"))
            .switchMap(rollerShutter -> telegramMessageService.getMessages()
                .map(TimeUtil::parseGermanDuration)
                .doOnError(throwable -> telegramMessageService.sendMessageToMainChannel("Das habe ich nicht verstanden. Nochmal von vorne mit /noAutomaticsForRollerShutter"))
                .doOnNext(period -> QuarkusTransaction.requiringNew().run(() -> {
                  var endDate = LocalDateTime.now().plus(period);
                  telegramMessageService.sendMessageToMainChannel("Alles klar, ich ignoriere die Störe bis " + GERMANY_DATE_TIME.format(endDate));
                  rollerShutterConfigRepository.save(rollerShutter.setNoAutomaticsUntil(endDate));
                }))
                .take(1)
            ))
        .subscribe());

    initTimer();
  }

  private void checkWindSpeed(Weather weather) {
    if (weather.getWind().isBiggerThan(10, METERS_PER_SECOND)) {
      telegramMessageService.sendMessageToMainChannel("Hui das ist sehr winding. Ich mach die Stören hoch.");
      deviceService.getDevicesOfType(RollerShutter.class)
          .forEach(RollerShutter::reportHighWind);
    }
  }

  private List<Completable> handleWeatherUpdate(Weather newWeather) {
    var configs = QuarkusTransaction.requiringNew().call(() -> rollerShutterConfigRepository.findRollerShutterConfigByCompassDirectionIsNotNull().toList());
    var newBrightness = newWeather.getLight().getAs(KILO_LUX).intValue();

    if (newBrightness > 250) {
      if (newWeather.getOuterTemperatur().isSmallerThan(15, DEGREE)) {
        return new ArrayList<>();
      }

      highSunLock.blockFor(Duration.ofHours(1));
      var sunDirection = QuarkusTransaction.requiringNew().call(weatherService::getCurrentSunDirection);
      var compassDirection = resolveCompassDirection(sunDirection);

      return configs.stream()
          .map(config -> handleHighBrightness(config, sunDirection, compassDirection))
          .toList();

    } else if (newBrightness < 70 && newBrightness > 10 && highSunLock.isGateOpen()) {
      return configs.stream()
          .filter(RollerShutterService::isOkToOpen)
          .map(this::getRollerShutter)
          .filter(RollerShutterService::hasNoManualAction)
          .map(RollerShutter::open)
          .toList();
    } else if (newBrightness == 0) {
      return configs.stream()
          .filter(RollerShutterService::isOkToClose)
          .map(this::getRollerShutter)
          .filter(RollerShutterService::hasNoManualAction)
          .map(RollerShutter::close)
          .toList();
    }

    return new ArrayList<>();
  }

  private static boolean hasNoManualAction(RollerShutter rollerShutter) {
    return is(Duration.between(rollerShutter.getLastManuallAction(), LocalDateTime.now()).abs()).biggerAs(TIMEOUT_AFTER_MANUAL);
  }

  @NotNull
  private Completable handleHighBrightness(RollerShutterConfig config, AzimuthZenithAngle sunDirection, CompassDirection compassDirection) {
    var rollerShutter = getRollerShutter(config);

    if (!hasNoManualAction(rollerShutter)) {
      return Completable.complete();
    }

    if (!isOkToOpen(config)) {
      return Completable.complete();
    }

    if (config.getCompassDirection().contains(compassDirection)) {
      if (sunDirection.getZenithAngle() > 40 && rollerShutter.getCurrentLift() > 50) {
        return rollerShutter.setLiftAndThenTilt(50, 40);
      } else if (sunDirection.getZenithAngle() > 20 && rollerShutter.getCurrentLift() > 75) {
        return rollerShutter.setLiftAndThenTilt(75, 75);
      }
    }

    return rollerShutter.open();
  }

  private static boolean isOkToOpen(RollerShutterConfig config) {
    //noinspection DataFlowIssue
    return (config.getOpenAt() == null || config.getOpenAt().isBefore(LocalTime.now()))
        && config.getNoAutomaticsUntil() == null || config.getNoAutomaticsUntil().isAfter(LocalDateTime.now());
  }

  private static boolean isOkToClose(RollerShutterConfig config) {
    //noinspection DataFlowIssue
    return (config.getCloseAt() == null || config.getCloseAt().isAfter(LocalTime.now()))
        && config.getNoAutomaticsUntil() == null || config.getNoAutomaticsUntil().isAfter(LocalDateTime.now());
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

    return Observable.timer(nextEvent.toSeconds(), SECONDS)
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
