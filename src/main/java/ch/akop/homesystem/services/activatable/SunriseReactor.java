package ch.akop.homesystem.services.activatable;

import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.services.impl.WeatherServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static ch.akop.homesystem.states.NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON;
import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

@Service
@RequiredArgsConstructor
public class SunriseReactor extends Activatable {

    private final WeatherService weatherService;
    private final DeviceService deviceService;
    private final MessageService messageService;
    private final BasicConfigRepository basicConfigRepository;


    @PostConstruct
    public void startForAllStates() {
        started();
    }

    @Override
    protected void started() {
        super.disposeWhenClosed(weatherService.getCurrentAndPreviousWeather()
                .subscribe(this::turnLightsOffWhenItIsGettingLight));
    }

    private void turnLightsOffWhenItIsGettingLight(WeatherServiceImpl.CurrentAndPreviousWeather weather) {
        if (weather.previous().getLight().isBiggerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)
                || weather.current().getLight().isSmallerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)) {
            return;
        }

        if (basicConfigRepository.findFirstByOrderByModifiedDesc().orElseThrow().isSendMessageWhenTurnLightsOff()) {
            messageService.sendMessageToMainChannel("Es wird hell, ich mach mal die Lichter aus.");
        }
        deviceService.turnAllLightsOff();
    }
}
