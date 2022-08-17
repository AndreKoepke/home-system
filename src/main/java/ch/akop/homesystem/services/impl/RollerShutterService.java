package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.light.LightUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.NoSuchElementException;

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
                                var isRollerShutterOpen = rollerShutter.getLift() > 50 || rollerShutter.getTilt() > 50;
                                var isRollerShutterHeadingToSun = true;
                                
                                if (isRollerShutterOpen
                                        && isRollerShutterHeadingToSun
                                        && weather.getLight().isBiggerThan(BigDecimal.valueOf(100), LightUnit.WATT_PER_SQUARE_METER)) {
                                    // close it
                                    rollerShutter.setLift(0);
                                    rollerShutter.setTilt(30);
                                } else if (isRollerShutterOpen && weather.getLight().isSmallerThan(BigDecimal.valueOf(50), LightUnit.WATT_PER_SQUARE_METER)) {

                                }

                            });

                });
    }

}
