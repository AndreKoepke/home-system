package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.sensor.PowerMeter;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PowerMeterService {

    private final HomeConfig homeConfig;
    private final DeviceService deviceService;
    private final FanService fanService;
    private final MessageService messageService;
    private final Map<HomeConfig.PowerMeterConfigs, State> stateMap;


    @PostConstruct
    public void setUpListener() {
        homeConfig.getPowerMeters()
                .forEach(powerMeterConfig -> {
                    var sensor = deviceService.findDeviceByName(powerMeterConfig.getSensorName(), PowerMeter.class)
                            .orElseThrow(() -> new IllegalStateException("Sensor %s not found.".formatted(powerMeterConfig.getSensorName())));

                    //noinspection ResultOfMethodCallIgnored
                    sensor.getCurrent$()
                            .map(current -> current > powerMeterConfig.getIsOnWhenMoreThan())
                            .distinctUntilChanged()
                            .debounce(5, TimeUnit.MINUTES)
                            .subscribe(isRunning -> {
                                var lastState = stateMap.getOrDefault(powerMeterConfig, new State().setRunning(false));

                                if (lastState.isRunning() != isRunning) {
                                    stateSwitched(powerMeterConfig, isRunning);
                                }

                                lastState.setRunning(isRunning);
                                lastState.setStartTime(LocalDateTime.now());
                            });
                });
    }

    private void stateSwitched(HomeConfig.PowerMeterConfigs powerMeterConfig, boolean isRunning) {
        if (isRunning) {
            messageService.sendMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOn());
            if (powerMeterConfig.getLinkToFan() != null) {
                fanService.startFan(powerMeterConfig.getLinkToFan());
            }
        } else {
            messageService.sendMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOff());

            if (powerMeterConfig.getLinkToFan() != null) {
                fanService.stopFan(powerMeterConfig.getLinkToFan());
            }
        }
    }

    @Data
    public static class State {
        private boolean running;
        private LocalDateTime startTime = LocalDateTime.now();
    }
}
