package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.util.EventConstants.BUTTON;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.events.ButtonPressEvent;
import ch.akop.homesystem.persistence.model.config.TimedButtonConfig;
import ch.akop.homesystem.persistence.repository.config.TimedButtonConfigRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.transaction.Transactional;
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
    if (runningJobs.containsKey(config.getButtonName())) {
      runningJobs.get(config.getButtonName()).dispose();
      runningJobs.put(config.getButtonName(), turnLightOffAfterConfiguratedTime(config));
    } else {
      forEveryLight(config, SimpleLight::turnOn);
      runningJobs.put(config.getButtonName(), turnLightOffAfterConfiguratedTime(config));
    }
  }

  @NotNull
  private Disposable turnLightOffAfterConfiguratedTime(TimedButtonConfig config) {
    return Observable.timer(config.getKeepOnFor().getSeconds(), TimeUnit.SECONDS)
        .take(1)
        .subscribe(aLong -> {
          runningJobs.remove(config.getButtonName());
          forEveryLight(config, SimpleLight::turnOff);
        });
  }

  private void forEveryLight(TimedButtonConfig config, Consumer<SimpleLight> action) {
    config.getLights().stream()
        .map(name -> deviceService.findDeviceByName(name, SimpleLight.class)
            .orElseThrow(() -> new NoSuchElementException("Light " + name + " not found")))
        .forEach(action);
  }
}
