package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.util.TimeUtil;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;

@Slf4j
@Service
@RequiredArgsConstructor
public class RollerShutterService {

    private final HomeSystemProperties homeSystemProperties;
    private final DeviceService deviceService;
    private final WeatherService weatherService;

    private final List<Disposable> disposables = new ArrayList<>();
    private final Map<LocalTime, List<HomeSystemProperties.RollerShutterConfig>> timeToConfigs = new HashMap<>();

    @PostConstruct
    private void initWeatherBasedRollerShutters() {
        homeSystemProperties.getRollerShutters().stream()
                .filter(config -> config.getCompassDirection() != null)
                .map(rollerShutterConfig -> weatherService.getCurrentAndPreviousWeather()
                        .subscribe(weather -> {
                            var currentComparedToPrevious = weather.current().getLight().compareTo(weather.previous().getLight().getAs(KILO_LUX));
                            if (currentComparedToPrevious < 0) {
                                itsGettingDarkerOutside(rollerShutterConfig, weather);
                            } else if (currentComparedToPrevious > 0) {
                                itsGettingBrighterOutside(rollerShutterConfig, weather);
                            }
                        }))
                .forEach(disposables::add);
    }

    @PostConstruct
    private void initTimer() {
        homeSystemProperties.getRollerShutters().stream()
                .filter(config -> config.getCloseAt() != null || config.getOpenAt() != null)
                .forEach(config -> {
                    Optional.ofNullable(config.getOpenAt())
                            .map(localTime -> timeToConfigs.computeIfAbsent(localTime, ignored -> new ArrayList<>()))
                            .ifPresent(list -> list.add(config));

                    Optional.ofNullable(config.getCloseAt())
                            .map(localTime -> timeToConfigs.computeIfAbsent(localTime, ignored -> new ArrayList<>()))
                            .ifPresent(list -> list.add(config));
                });

        if (timeToConfigs.isEmpty()) {
            // no defined times = nop
            return;
        }

        disposables.add(Observable.defer(this::timerForNextEvent)
                .repeat()
                .subscribe());
    }

    private Observable<LocalTime> timerForNextEvent() {
        var nextExecutionTime = getNextExecutionTime().atZone(ZoneId.systemDefault());
        var nextEvent = Duration.between(ZonedDateTime.now(), nextExecutionTime);

        return Observable.timer(nextEvent.toSeconds(), TimeUnit.SECONDS)
                .map(ignored -> nextExecutionTime.toLocalTime())
                .doOnNext(this::handleTime);
    }

    private void handleTime(LocalTime time) {
        timeToConfigs.get(time)
                .forEach(config -> {
                    var rollerShutter = getRollerShutter(config);
                    if (config.getCloseAt() != null && config.getCloseAt().equals(time)) {
                        log.info("Timed close for {}", rollerShutter.getName());
                        rollerShutter.setLiftAndThenTilt(0, 0);
                    } else {
                        log.info("Timed open for {}", rollerShutter.getName());
                        rollerShutter.setLiftAndThenTilt(100, 100);
                    }
                });
    }

    private LocalDateTime getNextExecutionTime() {
        return timeToConfigs
                .keySet()
                .stream()
                .map(TimeUtil::getLocalDateTimeForTodayOrTomorrow)
                .min(LocalDateTime::compareTo)
                .orElseThrow();
    }

    @PreDestroy
    private void tearDown() {
        disposables.forEach(Disposable::dispose);
    }


    private void itsGettingBrighterOutside(HomeSystemProperties.RollerShutterConfig config,
                                           WeatherServiceImpl.CurrentAndPreviousWeather weather) {
        var rollerShutter = getRollerShutter(config);

        if (weatherService.getCurrentSunDirection().equals(config.getCompassDirection())
                && lightIsGoingAboveThreshold(weather, 200)
                && weather.current().getOuterTemperatur().isBiggerThan(15, DEGREE)) {
            log.info("Weather close for {}", rollerShutter.getName());
            rollerShutter.setLiftAndThenTilt(0, 20);
        } else if (lightIsGoingAboveThreshold(weather, 50)) {
            log.info("Weather open for {}", rollerShutter.getName());
            rollerShutter.setLiftAndThenTilt(100, 100);
        }
    }

    private void itsGettingDarkerOutside(HomeSystemProperties.RollerShutterConfig config,
                                         WeatherServiceImpl.CurrentAndPreviousWeather weather) {
        var rollerShutter = getRollerShutter(config);

        if (lightIsGoingBelowThreshold(weather, 20)) {
            log.info("Weather close for {} because it is getting dark", rollerShutter.getName());
            rollerShutter.setLiftAndThenTilt(0, 0);
        } else if (lightIsGoingBelowThreshold(weather, 75)) {
            log.info("Weather open for {} because it is getting bright", rollerShutter.getName());
            rollerShutter.setLiftAndThenTilt(100, 100);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private boolean lightIsGoingAboveThreshold(WeatherServiceImpl.CurrentAndPreviousWeather weather, int threshold) {
        return weather.current().getLight().isBiggerThan(threshold, KILO_LUX)
                && weather.previous().getLight().isSmallerThan(threshold, KILO_LUX);
    }

    private boolean lightIsGoingBelowThreshold(WeatherServiceImpl.CurrentAndPreviousWeather weather, int threshold) {
        return weather.current().getLight().isSmallerThan(threshold, KILO_LUX)
                && weather.previous().getLight().isBiggerThan(threshold, KILO_LUX);
    }

    private RollerShutter getRollerShutter(HomeSystemProperties.RollerShutterConfig config) {
        return deviceService.findDeviceByName(config.getName(), RollerShutter.class)
                .orElseThrow(() -> new NoSuchElementException("No rollerShutter named '%s' was found in deviceList.".formatted(config.getName())));
    }
}
