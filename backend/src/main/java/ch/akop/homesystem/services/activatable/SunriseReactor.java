package ch.akop.homesystem.services.activatable;

import static ch.akop.homesystem.states.NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON;
import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.TelegramMessageService;
import ch.akop.homesystem.services.impl.WeatherService;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.RequestContextController;
import lombok.RequiredArgsConstructor;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class SunriseReactor extends Activatable {

  private final WeatherService weatherService;
  private final DeviceService deviceService;
  private final TelegramMessageService messageService;
  private final BasicConfigRepository basicConfigRepository;
  private final RequestContextController requestContextController;


  @PostConstruct
  void startForAllStates() {
    started();
  }

  @Override
  protected void started() {
    super.disposeWhenClosed(weatherService.getCurrentAndPreviousWeather()
        .subscribe(this::turnLightsOffWhenItIsGettingLight));
  }

  private void turnLightsOffWhenItIsGettingLight(WeatherService.CurrentAndPreviousWeather weather) {
    if (weather.previous().getLight().isBiggerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)
        || weather.current().getLight().isSmallerThan(THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)) {
      return;
    }

    requestContextController.activate();
    if (basicConfigRepository.findByOrderByModifiedDesc().orElseThrow().isSendMessageWhenTurnLightsOff()) {
      messageService.sendFunnyMessageToMainChannel("Es wird hell, ich mach mal die Lichter aus.");
    }
    deviceService.turnAllLightsOff();
    requestContextController.deactivate();
  }
}
