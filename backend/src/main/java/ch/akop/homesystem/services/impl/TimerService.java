package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.util.TimeUtil.determineNextExecutionTime;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.persistence.model.config.TimerConfig;
import ch.akop.homesystem.persistence.repository.config.TimerConfigRepository;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimerService {

  private final TimerConfigRepository configRepository;
  private final DeviceService deviceService;

  private final Map<LocalTime, Set<String>> timeToTurnOff = new HashMap<>();
  private final Map<LocalTime, Set<String>> timeToTurnOn = new HashMap<>();
  private Disposable turnOffSubscription;
  private Disposable turnOnSubscription;

  @Transactional
  public void init() {
    if (turnOffSubscription != null) {
      turnOffSubscription.dispose();
    }
    if (turnOnSubscription != null) {
      turnOnSubscription.dispose();
    }
    timeToTurnOn.clear();
    timeToTurnOff.clear();

    configRepository.findAll()
        .stream()
        .filter(config -> config.getTurnOnAt() != null || config.getTurnOffAt() != null)
        .forEach(config -> {
          ofNullable(config.getTurnOnAt())
              .map(localTime -> timeToTurnOn.computeIfAbsent(localTime, ignored -> new HashSet<>()))
              .ifPresent(list -> list.addAll(config.getDevices()));

          ofNullable(config.getTurnOffAt())
              .map(localTime -> timeToTurnOff.computeIfAbsent(localTime, ignored -> new HashSet<>()))
              .ifPresent(list -> list.addAll(config.getDevices()));
        });

    registerEachLightAsControlledLight();

    if (!timeToTurnOn.isEmpty()) {
      turnOnSubscription = Observable.defer(() -> timerForNextEvent(timeToTurnOn, SimpleLight::turnOn))
          .repeat()
          .subscribe();
    }

    if (!timeToTurnOff.isEmpty()) {
      turnOffSubscription = Observable.defer(() -> timerForNextEvent(timeToTurnOff, SimpleLight::turnOff))
          .repeat()
          .subscribe();
    }
  }

  private void registerEachLightAsControlledLight() {
    Stream.concat(
            timeToTurnOff.values().stream(),
            timeToTurnOn.values().stream()
        ).flatMap(Set::stream)
        .flatMap(lightName -> deviceService.findDeviceByName(lightName, SimpleLight.class).stream())
        .forEach(deviceService::registerAControlledLight);
  }

  @Transactional
  public void update(TimerConfig config) {
    configRepository.save(config);
    init();
  }

  @Transactional
  public List<TimerConfig> findAll() {
    return configRepository.findAll();
  }

  private Observable<LocalTime> timerForNextEvent(Map<LocalTime, Set<String>> timingMap, Consumer<SimpleLight> action) {
    var nextExecutionTime = determineNextExecutionTime(timingMap).atZone(ZoneId.systemDefault());
    var nextEvent = Duration.between(ZonedDateTime.now(), nextExecutionTime);

    return Observable.timer(nextEvent.toSeconds(), SECONDS)
        .map(ignored -> nextExecutionTime.toLocalTime())
        .doOnNext(timeKey -> handleTime(timingMap, timeKey, action));
  }

  private void handleTime(Map<LocalTime, Set<String>> map, LocalTime time, Consumer<SimpleLight> action) {
    map.get(time)
        .stream()
        .flatMap(deviceName -> deviceService.findDeviceByName(deviceName, SimpleLight.class).stream())
        .forEach(action);
  }
}
