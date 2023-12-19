package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.persistence.repository.config.LightnessControlledDeviceRepository;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.light.Light;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ApplicationScoped
public class LightnessControlledDeviceService {

  private final LightnessControlledDeviceRepository configRepository;
  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final TelegramMessageService telegramMessageService;
  private final Vertx vertx;

  void setupWeatherListener(@Observes StartupEvent startup) {
    var rxScheduler = RxHelper.blockingScheduler(vertx);
    weatherService.getWeather()
        .map(Weather::getLight)
        .subscribeOn(rxScheduler)
        .subscribe(this::handleWeatherUpdate);
  }

  private void handleWeatherUpdate(Light lightOutside) {
    configRepository.findAll()
        .forEach(config -> {
          if (config.isDarkerAs(lightOutside)) {
            tryTo(config.getName(), SimpleLight::turnOn);
          }

          if (config.isLighterAs(lightOutside)) {
            tryTo(config.getName(), SimpleLight::turnOff);
          }
        });
  }

  private void tryTo(String deviceName, Consumer<SimpleLight> action) {
    deviceService.findDeviceByName(deviceName, SimpleLight.class)
        .ifPresentOrElse(action,
            () -> telegramMessageService.sendMessageToMainChannel("Ich wollte grade das Licht '"
                + deviceName + "' anmachen, aber ich kann es nicht finden")
        );
  }
}
