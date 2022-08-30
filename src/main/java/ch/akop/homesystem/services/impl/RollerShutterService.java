package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;

@Service
@RequiredArgsConstructor
public class RollerShutterService {

    private final HomeSystemProperties homeSystemProperties;
    private final DeviceService deviceService;
    private final WeatherService weatherService;

    private List<Disposable> disposables = new ArrayList<>();

    @PostConstruct
    private void init() {
        disposables = homeSystemProperties.getRollerShutters()
                .stream()
                .map(rollerShutterConfig -> weatherService.getCurrentAndPreviousWeather()
                        .subscribe(weather -> {
                            var currentComparedToPrevious = weather.current().getLight().compareTo(weather.previous().getLight().getAs(KILO_LUX));
                            if (currentComparedToPrevious < 0) {
                                itsGettingDarkerOutside(rollerShutterConfig, weather.current());
                            } else if (currentComparedToPrevious > 0) {
                                itsGettingBrighterOutside(rollerShutterConfig, weather.current());
                            }
                        }))
                .toList();
    }

    @PreDestroy
    private void tearDown() {
        disposables.forEach(Disposable::dispose);
    }


    private void itsGettingBrighterOutside(HomeSystemProperties.RollerShutter config, Weather currentWeather) {
        var rollerShutter = getRollerShutter(config);
        var isOpen = rollerShutter.getCurrentLift() > 50 || rollerShutter.getCurrentTilt() > 50;

        if (isOpen
                && weatherService.getCurrentSunDirection().equals(config.getCompassDirection())
                && currentWeather.getLight().isBiggerThan(250, KILO_LUX)
                && currentWeather.getOuterTemperatur().isBiggerThan(15, DEGREE)) {
            rollerShutter.setLiftAndThenTilt(0, 20);
        }
    }

    private void itsGettingDarkerOutside(HomeSystemProperties.RollerShutter config, Weather currentWeather) {
        var rollerShutter = getRollerShutter(config);
        var isOpen = rollerShutter.getCurrentLift() > 50 || rollerShutter.getCurrentTilt() > 50;

        if (isOpen && currentWeather.getLight().isSmallerThan(20, KILO_LUX)) {
            rollerShutter.setLiftAndThenTilt(0, 0);
        } else if (!isOpen && currentWeather.getLight().isSmallerThan(75, KILO_LUX)) {
            rollerShutter.setLiftAndThenTilt(100, 100);
        }
    }

    private RollerShutter getRollerShutter(HomeSystemProperties.RollerShutter config) {
        return deviceService.findDeviceByName(config.getName(), RollerShutter.class)
                .orElseThrow(() -> new NoSuchElementException("No rollerShutter named '%s' was found in deviceList.".formatted(config.getName())));
    }
}
