package ch.akop.homesystem.services.activatable;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static ch.akop.homesystem.states.NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON;
import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

@Service
@RequiredArgsConstructor
public class SunriseReactor extends Activatable {

    private final WeatherService weatherService;
    private final DeviceService deviceService;
    private final MessageService messageService;
    private final HomeSystemProperties homeSystemProperties;

    private Weather previousWeather;

    @PostConstruct
    public void startForAllStates() {
        started();
    }

    @Override
    protected void started() {
        super.disposeWhenClosed(weatherService.getWeather()
                .doOnNext(this::turnLightsOffWhenItIsGettingLight)
                .doOnNext(weather -> previousWeather = weather)
                .subscribe());
    }

    private void turnLightsOffWhenItIsGettingLight(Weather weather) {
        if (previousWeather == null
                || previousWeather.getLight().isBiggerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)
                || weather.getLight().isSmallerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)) {
            return;
        }

        if (homeSystemProperties.isSendMessageWhenTurnLightsOff()) {
            messageService.sendMessageToMainChannel("Es wird hell, ich mach mal die Lichter aus.");
        }
        deviceService.turnAllLightsOff();
    }
}
