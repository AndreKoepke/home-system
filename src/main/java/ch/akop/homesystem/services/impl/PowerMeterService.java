package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.sensor.PowerMeter;
import ch.akop.homesystem.persistence.model.config.PowerMeterConfig;
import ch.akop.homesystem.persistence.repository.config.PowerMeterConfigRepository;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PowerMeterService {

    private final PowerMeterConfigRepository powerMeterConfigRepository;
    private final DeviceService deviceService;
    private final FanService fanService;
    private final MessageService messageService;

    private final Map<String, Boolean> configToIsRunningMap = new HashMap<>();


    @PostConstruct
    public void setUpListener() {
        // TODO restart when config changes
        powerMeterConfigRepository.findAll()
                .forEach(this::setupForConfig);
    }

    private void setupForConfig(PowerMeterConfig powerMeterConfig) {
        var sensor = deviceService.findDeviceByName(powerMeterConfig.getName(), PowerMeter.class)
                .orElseThrow(() -> new IllegalStateException("Sensor %s not found.".formatted(powerMeterConfig.getName())));

        //noinspection ResultOfMethodCallIgnored
        sensor.getCurrent$()
                .map(current -> current > powerMeterConfig.getIsOnWhenMoreThan())
                // reduce the re-emits of same values
                .distinctUntilChanged()
                // delay, when on-value not changed
                .debounce(1, TimeUnit.MINUTES)
                .distinctUntilChanged()
                .subscribe(isNowRunning -> {
                    var wasLastTimeRunning = configToIsRunningMap.getOrDefault(powerMeterConfig.getName(), false);

                    if (!Objects.equals(wasLastTimeRunning, isNowRunning)) {
                        stateSwitched(powerMeterConfig, isNowRunning);
                    }

                    configToIsRunningMap.put(powerMeterConfig.getName(), isNowRunning);
                });
    }

    private void stateSwitched(PowerMeterConfig powerMeterConfig, boolean isRunning) {
        if (isRunning) {
            messageService.sendMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOn());
            fanService.startFan(powerMeterConfig.getLinkedFan());
        } else {
            messageService.sendMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOff());
            fanService.stopFan(powerMeterConfig.getLinkedFan());
        }
    }
}
