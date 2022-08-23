package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.NoSuchElementException;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

@Service
@RequiredArgsConstructor
public class RollerShutterService {

    private final HomeSystemProperties homeSystemProperties;
    private final DeviceService deviceService;
    private final WeatherService weatherService;

    @PostConstruct
    private void init() {
        homeSystemProperties.getRollerShutters()
                .forEach(rollerShutterConfig -> {
                    var rollerShutter = deviceService.findDeviceByName(rollerShutterConfig.getName(), RollerShutter.class)
                            .orElseThrow(() -> new NoSuchElementException("No rollerShutter named '%s' was found in deviceList.".formatted(rollerShutterConfig.getName())));

                    weatherService.getWeather()
                            .subscribe(weather -> {
                                var isRollerShutterOpen = rollerShutter.getCurrentLift() > 50 || rollerShutter.getCurrentTilt() > 50;
                                // TODO tbd
                                var isRollerShutterHeadingToSun = true;

                                if (isRollerShutterOpen) {
                                    // we want to close it when it's getting very bright
                                    // or when the nights start

                                    if (isRollerShutterHeadingToSun && weather.getLight().isBiggerThan(250, KILO_LUX)) {
                                        rollerShutter.setLiftAndThenTilt(0, 30);
                                    } else if (weather.getLight().isSmallerThan(20, KILO_LUX)) {
                                        rollerShutter.setLiftAndThenTilt(0, 20);
                                    }
                                } else {
                                    // we want to open it, when it is not so bright anymore

                                }

                            });

                });
    }

}
