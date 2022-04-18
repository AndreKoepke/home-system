package ch.akop.homesystem.message;

import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;

import static ch.akop.homesystem.states.NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON;
import static ch.akop.weathercloud.light.LightUnit.WATT_PER_SQUARE_METER;
import static java.time.temporal.ChronoUnit.SECONDS;

@Service
@RequiredArgsConstructor
public class SunriseReactor extends Activatable {

    private final WeatherService weatherService;
    private final DeviceService deviceService;
    private final MessageService messageService;

    private Weather previousWeather;

    @PostConstruct
    public void startForAllStates() {
        this.started();
    }

    @Override
    protected void started() {
        super.disposeWhenClosed(this.weatherService.getWeather()
                .doOnNext(this::turnLightsOffWhenItIsGettingLight)
                .doOnNext(weather -> this.previousWeather = weather)
                .subscribe());
    }

    private void turnLightsOffWhenItIsGettingLight(final Weather weather) {
        if (this.previousWeather == null
                || this.previousWeather.getLight().isBiggerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, WATT_PER_SQUARE_METER)
                || weather.getLight().isSmallerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, WATT_PER_SQUARE_METER)) {
            return;
        }

        this.messageService.sendMessageToMainChannel("Es wird hell, ich mach mal die Lichter aus.");
        this.deviceService.getDevicesOfType(Light.class)
                .forEach(light -> light.setBrightness(0, Duration.of(20, SECONDS)));
    }
}
