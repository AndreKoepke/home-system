package ch.akop.homesystem.services.activatable;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.StateService;
import ch.akop.homesystem.services.impl.TelegramMessageService;
import ch.akop.homesystem.services.impl.UserService;
import ch.akop.homesystem.services.impl.WeatherService;
import ch.akop.homesystem.states.HolidayState;
import ch.akop.homesystem.states.NormalState;
import ch.akop.weathercloud.Weather;
import io.quarkus.runtime.Startup;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.RequestContextController;
import lombok.RequiredArgsConstructor;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class SunsetReactor extends Activatable {

  private final WeatherService weatherService;
  private final TelegramMessageService messageService;
  private final DeviceService deviceService;
  private final BasicConfigRepository basicConfigRepository;
  private final UserService userService;
  private final RequestContextController requestContextController;
  private final StateService stateService;


  private Weather previousWeather;

  @Override
  protected void started() {
    super.disposeWhenClosed(weatherService.getWeather()
        .doOnNext(this::turnLightsOnWhenItIsGettingDark)
        .doOnNext(weather -> previousWeather = weather)
        .subscribe());
  }

  private void turnLightsOnWhenItIsGettingDark(Weather weather) {

    if (previousWeather == null
        || previousWeather.getLight().isSmallerThan(NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)
        || weather.getLight().isBiggerThan(NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON, KILO_LUX)) {
      return;
    }

    if (stateService.isState(HolidayState.class)) {
      activeSunsetScenes();
      return;
    }

    if (stateService.isState(NormalState.class)) {

      if (!userService.isAnyoneAtHome()) {
        messageService.sendMessageToMainChannel("Es wird dunkel ... aber weil keiner Zuhause ist, mache ich mal nichts.");
        return;
      }

      messageService.sendMessageToMainChannel("Es wird dunkel ... ich mach mal etwas Licht. Es sei denn ... /keinlicht");
      super.disposeWhenClosed(messageService.getMessages()
          .filter(message -> message.startsWith("/keinlicht"))
          .take(1)
          .timeout(5, TimeUnit.MINUTES)
          .subscribe(s -> {
          }, ignored -> activeSunsetScenes()));
    }
  }

  private void activeSunsetScenes() {
    requestContextController.activate();
    deviceService.getDevicesOfType(Group.class)
        .stream()
        .filter(this::areAllLampsAreOff)
        .flatMap(group -> group.getScenes().stream())
        .filter(scene -> scene.getName().equals(basicConfigRepository.findByOrderByModifiedDesc()
            .orElseThrow()
            .getSunsetSceneName()))
        .forEach(Scene::activate);
    requestContextController.deactivate();
  }

  private boolean areAllLampsAreOff(Group group) {
    return deviceService.getDevicesOfType(SimpleLight.class)
        .stream()
        .filter(light -> group.getLights().contains(light.getId()))
        .noneMatch(SimpleLight::isCurrentStateIsOn);
  }
}
