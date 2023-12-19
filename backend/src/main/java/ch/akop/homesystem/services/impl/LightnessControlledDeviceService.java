package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.persistence.model.config.LightnessControlledDeviceConfig;
import ch.akop.homesystem.persistence.repository.config.LightnessControlledDeviceRepository;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.light.Light;
import io.quarkus.runtime.StartupEvent;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ApplicationScoped
public class LightnessControlledDeviceService {

  private final LightnessControlledDeviceRepository configRepository;
  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final TelegramMessageService telegramMessageService;
  private final ManagedExecutor executor;

  void setupWeatherListener(@Observes StartupEvent startup) {
    weatherService.getWeather()
        .map(Weather::getLight)
        .subscribe(this::handleWeatherUpdate);
  }

  private void handleWeatherUpdate(Light lightOutside) {
    executor.runAsync(() -> configRepository.findAll()
        .forEach(config -> handleConfig(lightOutside, config)));
  }

  private void handleConfig(Light lightOutside, LightnessControlledDeviceConfig config) {
    if (config.isDarkerAs(lightOutside)) {
      tryTo(config.getName(), SimpleLight::turnOn);
    }

    if (config.isLighterAs(lightOutside)) {
      tryTo(config.getName(), SimpleLight::turnOff);
    }
  }

  private void tryTo(String deviceName, Consumer<SimpleLight> action) {
    deviceService.findDeviceByName(deviceName, SimpleLight.class)
        .ifPresentOrElse(action,
            () -> telegramMessageService.sendMessageToMainChannel("Ich wollte grade das Licht '"
                + deviceName + "' anmachen, aber ich kann es nicht finden")
        );
  }
}
