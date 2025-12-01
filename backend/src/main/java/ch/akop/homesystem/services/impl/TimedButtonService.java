package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.util.EventConstants.BUTTON;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.events.ButtonPressEvent;
import ch.akop.homesystem.persistence.model.config.TimedButtonConfig;
import ch.akop.homesystem.persistence.repository.config.TimedButtonConfigRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimedButtonService {

  private final TimedButtonConfigRepository configRepository;
  private final DeviceService deviceService;
  private final Map<String, Disposable> runningJobs = new HashMap<>();

  @Transactional
  @ConsumeEvent(value = BUTTON, blocking = true)
  public void buttonEventHandler(ButtonPressEvent event) {
    configRepository.findAllByButtonNameAndButtonEvent(event.getButtonName(), event.getButtonEvent())
        .forEach(this::handleButtonPressed);
  }

  private void handleButtonPressed(TimedButtonConfig config) {
    var isAlreadyRunning = runningJobs.containsKey(config.getButtonName());
    log.info("Timed button {} was pressed and is {}", config.getButtonName(), isAlreadyRunning ? "already running" : "not running");
    forEveryLight(config, SimpleLight::turnOn);
    if (isAlreadyRunning) {
      runningJobs.get(config.getButtonName()).dispose();
    }
    runningJobs.put(config.getButtonName(), turnLightOffAfterConfiguratedTime(config));
  }

  @NotNull
  private Disposable turnLightOffAfterConfiguratedTime(TimedButtonConfig config) {
    return Observable.timer(config.getKeepOnFor().getSeconds(), TimeUnit.SECONDS)
        .take(1)
        .subscribe(aLong -> {
          log.info("Turning timed lights off (buttonName={})", config.getButtonName());
          runningJobs.remove(config.getButtonName());
          forEveryLight(config, SimpleLight::turnOff);
        });
  }

  private void forEveryLight(TimedButtonConfig config, Consumer<SimpleLight> action) {
    config.getLights().stream()
        .flatMap(name -> deviceService.findDeviceByName(name, SimpleLight.class)
            .or(() -> {
              log.warn("No light found for TimedButtonConfig (buttonName={},light={})", config.getButtonName(), name);
              return Optional.empty();
            })
            .stream())
        .forEach(action);
  }
}
