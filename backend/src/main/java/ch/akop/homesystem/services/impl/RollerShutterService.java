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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
  private static final DateTimeFormatter GERMANY_DATE_TIME = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
      .withLocale(Locale.GERMANY);

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

    if (newBrightness > 400) {
      if (newWeather.getOuterTemperatur().isSmallerThan(15, DEGREE)) {
        return new ArrayList<>();
      }

      highSunLock.blockFor(Duration.ofHours(1));
      var sunDirection = QuarkusTransaction.requiringNew().call(weatherService::getCurrentSunDirection);
      var compassDirection = resolveCompassDirection(sunDirection);

      return configs.stream()
          .map(config -> handleHighBrightness(config, sunDirection, compassDirection))
          .toList();

    } else if (newBrightness < 200 && newBrightness > 10 && highSunLock.isGateOpen()) {
      return configs.stream()
          .filter(RollerShutterService::isOkToOpen)
          .map(this::getRollerShutter)
          .filter(RollerShutterService::hasNoManualAction)
          .map(RollerShutter::open)
          .toList();

