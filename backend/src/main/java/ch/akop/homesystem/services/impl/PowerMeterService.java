package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.PowerMeter;
import ch.akop.homesystem.persistence.model.config.PowerMeterConfig;
import ch.akop.homesystem.persistence.repository.config.PowerMeterConfigRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PowerMeterService {

  private final PowerMeterConfigRepository powerMeterConfigRepository;
  private final DeviceService deviceService;
  private final FanService fanService;
  private final TelegramMessageService messageService;

  private final Map<String, Boolean> configToIsRunningMap = new HashMap<>();
  private final Map<String, Integer> pauses = new HashMap<>();


  @Transactional
  public void init() {
    // TODO restart when config changes
    powerMeterConfigRepository.findAll()
        .forEach(this::setupForConfig);
  }

  private void setupForConfig(PowerMeterConfig powerMeterConfig) {
    var sensor = deviceService.findDeviceByName(powerMeterConfig.getName(), PowerMeter.class)
        .orElseThrow(() -> new IllegalStateException("Sensor %s not found.".formatted(powerMeterConfig.getName())));

    deviceService.findDeviceByName(powerMeterConfig.getName(), SimpleLight.class)
        .ifPresent(actor -> actor.getState$()
            .filter(isOn -> !isOn)
            .subscribe(isOn -> pauses.replace(powerMeterConfig.getName(), 0)));

    //noinspection ResultOfMethodCallIgnored
    sensor.getCurrent$()
        .map(current -> current > powerMeterConfig.getIsOnWhenMoreThan())
        // reduce the re-emits of same values
        .distinctUntilChanged()
        // delay, when on-value not changed
        .debounce(1, TimeUnit.MINUTES)
        .distinctUntilChanged()
        .filter(isNowRunning -> !makingAPause(powerMeterConfig, isNowRunning))
        .subscribe(isNowRunning -> {
          var wasLastTimeRunning = configToIsRunningMap.getOrDefault(powerMeterConfig.getName(), false);

          if (!Objects.equals(wasLastTimeRunning, isNowRunning)) {
            stateSwitched(powerMeterConfig, isNowRunning);
          }

          if (wasLastTimeRunning && !isNowRunning && powerMeterConfig.getTurnOffWhenReady()) {
            deviceService.findDeviceByName(powerMeterConfig.getName(), SimpleLight.class)
                .ifPresent(SimpleLight::turnOff);
          }

          configToIsRunningMap.put(powerMeterConfig.getName(), isNowRunning);
        });
  }


  private boolean makingAPause(PowerMeterConfig config, boolean isNowRunning) {

    var wasLastTimeRunning = configToIsRunningMap.get(config.getName());

    if (config.getPausesDuringRun() == null || wasLastTimeRunning == null) {
      return false;
    }

    var alreadyNoticedPauses = pauses.getOrDefault(config.getName(), 0);
    if (wasLastTimeRunning && !isNowRunning && alreadyNoticedPauses < config.getPausesDuringRun()) {
      // starts a pause
      pauses.put(config.getName(), alreadyNoticedPauses + 1);
      configToIsRunningMap.put(config.getName(), false);
      return true;
    } else if (!wasLastTimeRunning && isNowRunning) {
      // finished a pause
      configToIsRunningMap.put(config.getName(), true);
      return true;
    } else if (wasLastTimeRunning && !isNowRunning) {
      // ready
      pauses.remove(config.getName());
      return false;
    }

    log.warn("Never should get into the code. PowerMeter '{}' is {} and before it {}",
        config.getName(),
        isNowRunning ? "RUNNING" : "NOT RUNNING",
        wasLastTimeRunning ? "RUNS" : "NOT RUNS");
    return false;
  }

  private void stateSwitched(PowerMeterConfig powerMeterConfig, boolean isRunning) {
    if (isRunning) {
      messageService.sendFunnyMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOn());
      fanService.startFan(powerMeterConfig.getLinkedFan());
    } else {
      messageService.sendFunnyMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOff());
      fanService.stopFan(powerMeterConfig.getLinkedFan());
    }
  }
}
