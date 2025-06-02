package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.models.devices.actor.RollerShutter.BLOCK_TIME_WHEN_HIGH_WIND;
import static ch.akop.homesystem.util.Comparer.is;
import static ch.akop.homesystem.util.TimeUtil.getLocalDateTimeForTodayOrTomorrow;
import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;
import static ch.akop.weathercloud.wind.WindSpeedUnit.METERS_PER_SECOND;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.persistence.repository.config.RollerShutterConfigRepository;
import ch.akop.homesystem.services.activatable.Activatable;
import ch.akop.homesystem.util.TimeUtil;
import ch.akop.homesystem.util.TimedGateKeeper;
import ch.akop.weathercloud.Weather;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.rxjava3.RxHelper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.e175.klaus.solarpositioning.SolarPosition;
import org.jetbrains.annotations.NotNull;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RollerShutterService extends Activatable {

  public static final Duration TIMEOUT_AFTER_MANUAL = Duration.ofHours(1);
  public static final Duration KEEP_OPEN_AFTER_DARKNESS_FOR = Duration.ofMinutes(10);
  private static final DateTimeFormatter GERMANY_DATE_TIME = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
      .withLocale(Locale.GERMANY);

  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final RollerShutterConfigRepository rollerShutterConfigRepository;
  private final TelegramMessageService telegramMessageService;
  private final Vertx vertx;
  private final EventBus eventBus;

  private final Map<LocalTime, Set<String>> timeToConfigs = new HashMap<>();
  private final TimedGateKeeper highSunLock = new TimedGateKeeper();

  private Boolean blockedByUser = false;
  private final Subject<Boolean> blockedByUserResolver = PublishSubject.create();

  @Transactional
  public void init() {
    started();
  }

  @Override
  protected void started() {
    linkConfigsToRollerShutter();

    var rxScheduler = RxHelper.blockingScheduler(vertx, false);
    super.disposeWhenClosed(weatherService.getWeather()
        .subscribeOn(rxScheduler)
        .doOnNext(this::checkWindSpeed)
        .mergeWith(telegramMessageService.waitForMessageOnce("calcRollerShutter")
            .repeat()
            .switchMap(message -> weatherService.getWeather().take(1))
        )
        .mergeWith(blockedByUserResolver.switchMap(message -> weatherService.getWeather().take(1)))
        .debounce(10, SECONDS)
        .flatMapCompletable(weather -> Completable.merge(handleWeatherUpdate(weather)))
        .retryWhen(origin -> origin
            .doOnNext(throwable -> log.error("Error while setting rollerShutters. Retrying in 5min", throwable))
            .delay(5, TimeUnit.MINUTES))
        .subscribe());

    super.disposeWhenClosed(telegramMessageService.waitForMessageOnce("noAutomaticsForRollerShutter")
        .subscribeOn(rxScheduler)
        .doOnNext(message -> telegramMessageService.sendMessageToMainChannel("Ok, welche Störe soll ich eine Zeit in Ruhe lassen?"))
        .doOnNext(message -> deviceService.getDevicesOfType(RollerShutter.class)
            .forEach(rollerShutter -> telegramMessageService.sendMessageToMainChannel(rollerShutter.getName())))
        .switchMap(ignoredMessage -> telegramMessageService.getMessages()
            .map(messageTarget -> deviceService.findDeviceByName(messageTarget, RollerShutter.class))
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
                  telegramMessageService.sendMessageToMainChannel("Alles klar, ich ignoriere die Störe bis " + endDate.format(GERMANY_DATE_TIME));
                  rollerShutterConfigRepository.save(rollerShutter.setNoAutomaticsUntil(endDate));
                }))
                .take(1)
            ))
        .repeat()
        .subscribe());

    super.disposeWhenClosed(telegramMessageService.waitForMessageOnce("keineSonne")
        .switchMap(message -> {
          telegramMessageService.sendFunnyMessageToMainChannel("Ok ok, ich lasse die Stören bis zum Abend in Ruhe.");
          blockedByUser = true;
          return weatherService.getWeather();
        })
        .filter(weather -> weather.getLight().isSmallerThan(1d, KILO_LUX))
        .mergeWith(blockedByUserResolver.map(ignored -> new Weather()))
        .take(1)
        .doOnNext(weather -> {
          blockedByUser = false;
          telegramMessageService.sendFunnyMessageToMainChannel("Es wird dunkel, ich übernehme die Stören wieder.");
        })
        .repeat()
        .subscribe());

    initTimer();
  }

  public void startCalculatingAgain() {
    blockedByUserResolver.onNext(true);
  }

  @Transactional
  public void block(String id) {
    deviceService.findDeviceById(id, RollerShutter.class)
        .ifPresent(rollerShutter -> {
          var rollerShutterConfig = rollerShutterConfigRepository.findByNameLike(rollerShutter.getName());

          if (rollerShutterConfig.isPresent()) {
            rollerShutterConfig.get().setNoAutomaticsUntil(LocalDateTime.now().plusDays(1));
            telegramMessageService.sendMessageToMainChannel("Aye, " + rollerShutter.getName() + " ist bis morgen gesperrt.");
            super.dispose();
            super.start();
            eventBus.publish("devices/roller-shutters/update", id);
          }
        });
  }

  @Transactional
  public void unblock(String id) {
    deviceService.findDeviceById(id, RollerShutter.class)
        .ifPresent(rollerShutter -> {
          var rollerShutterConfig = rollerShutterConfigRepository.findByNameLike(rollerShutter.getName());

          if (rollerShutterConfig.isPresent()) {
            rollerShutterConfig.get().setNoAutomaticsUntil(null);
            telegramMessageService.sendMessageToMainChannel("Aye, " + rollerShutter.getName() + " ist wieder aktiv..");
            super.dispose();
            super.start();
            eventBus.publish("devices/roller-shutters/update", id);
          }
        });
  }

  private void linkConfigsToRollerShutter() {
    rollerShutterConfigRepository.findAll()
        .forEach(rollerShutterConfig -> deviceService.findDeviceByName(rollerShutterConfig.getName(), RollerShutter.class)
            .orElseThrow(() -> new IllegalStateException("RollerShutterConfig " + rollerShutterConfig.getName() + " was not found."))
            .setConfig(rollerShutterConfig));
  }

  private void checkWindSpeed(Weather weather) {
    if (weather.getWind().isBiggerThan(10, METERS_PER_SECOND)) {
      telegramMessageService.sendMessageToMainChannel("Hui das ist sehr winding. Ich mach die Stören hoch.");
      var configs = QuarkusTransaction.requiringNew().call(() -> rollerShutterConfigRepository.findRollerShutterConfigByCompassDirectionIsNotNull().toList());
      deviceService.getDevicesOfType(RollerShutter.class)
          .forEach(restoreAfter -> {
            var openAfterWindAlert = configs.stream()
                .filter(config -> config.getCloseAt() != null && config.getOpenAt() != null)
                .filter(config -> {
                  var windAlertEndsAt = LocalDateTime.now().plus(BLOCK_TIME_WHEN_HIGH_WIND);

                  return windAlertEndsAt.isBefore(getLocalDateTimeForTodayOrTomorrow(config.getOpenAt()))
                      && windAlertEndsAt.isAfter(getLocalDateTimeForTodayOrTomorrow(config.getCloseAt()));
                })
                .map(config -> true)
                .findFirst()
                .orElse(false);
            restoreAfter.reportHighWind(openAfterWindAlert);
          });
    }
  }

  private List<Completable> handleWeatherUpdate(Weather weather) {
    var rollerShutters = deviceService.getDevicesOfType(RollerShutter.class);
    var newBrightness = weather.getLight().getAs(KILO_LUX).intValue();

    if (weather.getOuterTemperatur().isBiggerThan(27, DEGREE)) {
      return handleHighTemperature(rollerShutters);
    }

    if (newBrightness > 300) {
      highSunLock.blockFor(Duration.ofMinutes(30));
    }

    var sunDirection = QuarkusTransaction.requiringNew().call(weatherService::getCurrentSunDirection);
    var compassDirection = resolveCompassDirection(sunDirection);

    log.info("Sun angles. Zenith %3.0f Azimuth %3.0f (%s)".formatted(sunDirection.zenithAngle(),
        sunDirection.azimuth(),
        compassDirection));

    return rollerShutters.stream()
        .filter(rollerShutter -> rollerShutter.getConfig() != null)
        .map(config -> handleWeatherUpdate(config, sunDirection, compassDirection, weather))
        .toList();
  }

  @NotNull
  private List<Completable> handleHighTemperature(Collection<RollerShutter> rollerShutters) {
    return rollerShutters.stream()
        .filter(rollerShutter -> rollerShutter.getConfig() != null)
        .filter(RollerShutterService::isOkToClose)
        .filter(RollerShutterService::hasNoManualAction)
        .filter(rollerShutter -> rollerShutter.getCurrentLift() > 10)
        .map(rollerShutter -> rollerShutter.setLiftAndThenTilt(10, 15, "high temperature"))
        .toList();
  }

  private static boolean hasNoManualAction(RollerShutter rollerShutter) {
    return is(Duration.between(rollerShutter.getLastManuallAction(), LocalDateTime.now()).abs()).biggerAs(TIMEOUT_AFTER_MANUAL);
  }

  @NotNull
  private Completable handleWeatherUpdate(RollerShutter rollerShutter,
      SolarPosition sunDirection,
      CompassDirection compassDirection,
      Weather weather) {
    var light = weather.getLight();

    if (!hasNoManualAction(rollerShutter) || !isOkToOpen(rollerShutter)) {
      return Completable.complete();
    }

    if (light.isBiggerThan(rollerShutter.getConfig().getHighSunLevel(), KILO_LUX)) {
      if (!rollerShutter.getConfig().getCompassDirection().contains(compassDirection)) {
        return rollerShutter.open("wrong compass direction");
      }
      return openBasedOnZenithAngle(rollerShutter, sunDirection.zenithAngle());
    } else if (light.isBiggerThan(10, KILO_LUX) && highSunLock.isGateOpen()) {
      return rollerShutter.open("not much light outside");
    } else if (isOkToClose(rollerShutter) && weatherService.outSideDarkFor().compareTo(KEEP_OPEN_AFTER_DARKNESS_FOR) > 0) {
      return rollerShutter.close("night");
    }

    return Completable.complete();
  }

  private Completable openBasedOnZenithAngle(RollerShutter rollerShutter, double zenithAngle) {
    var config = rollerShutter.getConfig();
    if (zenithAngle > 70) {
      return rollerShutter.setLiftAndThenTilt(0, config.getCloseLevelLowTilt(), "brightness and high zenith angle");
    } else if (zenithAngle > 40) {
      return rollerShutter.setLiftAndThenTilt(config.getCloseLevelLowLift(), config.getCloseLevelLowTilt(), "brightness");
    } else if (zenithAngle > 20) {
      return rollerShutter.setLiftAndThenTilt(config.getCloseLevelHighLift(), config.getCloseLevelHighTilt(), "brightness");
    }

    return Completable.complete();
  }

  private boolean isOkToOpen(RollerShutter rollerShutter) {
    var config = rollerShutter.getConfig();
    return (config.getOpenAt() == null || config.getOpenAt().isBefore(LocalTime.now()))
        && (config.getNoAutomaticsUntil() == null || config.getNoAutomaticsUntil().isBefore(LocalDateTime.now()))
        && !blockedByUser;
  }

  private static boolean isOkToClose(RollerShutter rollerShutter) {
    var config = rollerShutter.getConfig();
    return (config.getCloseAt() == null || config.getCloseAt().isAfter(LocalTime.now()))
        && (config.getNoAutomaticsUntil() == null || config.getNoAutomaticsUntil().isBefore(LocalDateTime.now()));
  }

  private void initTimer() {
    deviceService.getDevicesOfType(RollerShutter.class)
        .stream()
        .map(RollerShutter::getConfig)
        .filter(Objects::nonNull)
        .filter(config -> config.getCloseAt() != null || config.getOpenAt() != null)
        .forEach(config -> {
          ofNullable(config.getOpenAt())
              .map(localTime -> timeToConfigs.computeIfAbsent(localTime, ignored -> new HashSet<>()))
              .ifPresent(list -> list.add(config.getName()));

          ofNullable(config.getCloseAt())
              .map(localTime -> timeToConfigs.computeIfAbsent(localTime, ignored -> new HashSet<>()))
              .ifPresent(list -> list.add(config.getName()));
        });

    if (timeToConfigs.isEmpty()) {
      // no defined times = nop
      return;
    }

    super.disposeWhenClosed(Observable.defer(this::timerForNextEvent)
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
        .flatMap(name -> deviceService.findDeviceByName(name, RollerShutter.class).stream())
        .forEach(rollerShutter -> {
          if (rollerShutter.getConfig().getCloseAt() != null && rollerShutter.getConfig().getCloseAt().equals(time)) {
            rollerShutter.close("time").subscribe();
          } else {
            rollerShutter.open("time").subscribe();
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

  private CompassDirection resolveCompassDirection(SolarPosition sunDirection) {
    return Arrays.stream(CompassDirection.values())
        .min(Comparator.comparing(value -> Math.abs(value.getDirection() - sunDirection.azimuth())))
        .orElseThrow(() -> new NoSuchElementException("Can't resolve direction for %s".formatted(sunDirection)));
  }
}
