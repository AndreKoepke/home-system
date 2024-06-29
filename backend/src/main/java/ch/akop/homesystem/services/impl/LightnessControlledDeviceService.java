package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.persistence.model.config.LightnessControlledDeviceConfig;
import ch.akop.homesystem.persistence.repository.config.LightnessControlledDeviceRepository;
import ch.akop.weathercloud.Weather;
import ch.akop.weathercloud.light.Light;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.util.function.Consumer;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LightnessControlledDeviceService {

  private final LightnessControlledDeviceRepository configRepository;
  private final DeviceService deviceService;
  private final WeatherService weatherService;
  private final TelegramMessageService telegramMessageService;
  private final Vertx vertx;

  @Transactional
  public void init() {
    var rxScheduler = RxHelper.blockingScheduler(vertx);
    weatherService.getWeather()
        .map(Weather::getLight)
        .subscribeOn(rxScheduler)
        .subscribe(this::handleWeatherUpdate);
  }

  private void handleWeatherUpdate(Light lightOutside) {
    QuarkusTransaction.requiringNew().run(() -> {
      var all = configRepository.findAll();
      all.forEach(config -> handleConfig(lightOutside, config));
    });
  }

  private void handleConfig(Light lightOutside, LightnessControlledDeviceConfig config) {
    if (config.isLighterAs(lightOutside) || !config.isTimeOkForBeingOn()) {
      tryTo(config.getName(), SimpleLight::turnOff);
    } else if (config.isDarkerAs(lightOutside)) {
      tryTo(config.getName(), SimpleLight::turnOn);
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
