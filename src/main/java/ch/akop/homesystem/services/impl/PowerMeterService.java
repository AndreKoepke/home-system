package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.sensor.PowerMeter;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import io.reactivex.rxjava3.core.Observable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PowerMeterService {

    private final HomeSystemProperties homeSystemProperties;
    private final DeviceService deviceService;
    private final FanService fanService;
    private final MessageService messageService;
    
    private final Map<HomeSystemProperties.PowerMeterConfigs, Boolean> configToIsRunningMap = new HashMap<>();


    @PostConstruct
    public void setUpListener() {
        homeSystemProperties.getPowerMeters()
                .forEach(this::setupForConfig);
    }

    private void setupForConfig(HomeSystemProperties.PowerMeterConfigs powerMeterConfig) {
        var sensor = deviceService.findDeviceByName(powerMeterConfig.getSensorName(), PowerMeter.class)
                .orElseThrow(() -> new IllegalStateException("Sensor %s not found.".formatted(powerMeterConfig.getSensorName())));

        //noinspection ResultOfMethodCallIgnored
        sensor.getCurrent$()
                .map(current -> current > powerMeterConfig.getIsOnWhenMoreThan())
                // reduce the re-emits of same values
                .distinctUntilChanged()
                // delay, when on-value not changed
                .switchMap(this::delayIfFalse)
                .distinctUntilChanged()
                .subscribe(isNowRunning -> {
                    var wasLastTimeRunning = configToIsRunningMap.getOrDefault(powerMeterConfig, false);

                    if (!Objects.equals(wasLastTimeRunning, isNowRunning)) {
                        stateSwitched(powerMeterConfig, isNowRunning);
                    }

                    configToIsRunningMap.put(powerMeterConfig, isNowRunning);
                });
    }

    private Observable<Boolean> delayIfFalse(Boolean bool) {
        if (bool) {
            return Observable.just(true).delay(5, TimeUnit.MINUTES);
        } else {
            return Observable.just(false);
        }
    }

    private void stateSwitched(HomeSystemProperties.PowerMeterConfigs powerMeterConfig, boolean isRunning) {
        if (isRunning) {
            messageService.sendMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOn());
            fanService.startFan(powerMeterConfig.getLinkToFan());
        } else {
            messageService.sendMessageToMainChannel(powerMeterConfig.getMessageWhenSwitchOff());
            fanService.stopFan(powerMeterConfig.getLinkToFan());
        }
    }
}
