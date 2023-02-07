package ch.akop.homesystem.services.activatable;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.TelegramMessageService;
import ch.akop.homesystem.services.impl.UserService;
import ch.akop.homesystem.services.impl.WeatherService;
import ch.akop.homesystem.states.NormalState;
import ch.akop.weathercloud.Weather;
import io.quarkus.runtime.Startup;
import lombok.RequiredArgsConstructor;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.TimeUnit;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class SunsetReactor extends Activatable {

    private final WeatherService weatherService;
    private final TelegramMessageService messageService;
    private final DeviceService deviceService;
    private final BasicConfigRepository basicConfigRepository;
    private final UserService userService;

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

        if (!userService.isAnyoneAtHome()) {
            messageService.sendMessageToMainChannel("Es wird dunkel ... aber weil keiner Zuhause ist, mache ich mal nichts.");
            return;
        }

        messageService.sendMessageToMainChannel("Es wird dunkel ... ich mach mal etwas Licht. Es sei denn ... /keinlicht");
        super.disposeWhenClosed(messageService.getMessages()
                .filter("/keinlicht"::equalsIgnoreCase)
                .take(1)
                .timeout(5, TimeUnit.MINUTES)
                .subscribe(s -> {
                }, this::activeSunsetScenes));
    }

    private void activeSunsetScenes(Throwable ignored) {
        deviceService.getDevicesOfType(Group.class)
                .stream()
                .filter(this::areAllLampsAreOff)
                .flatMap(group -> group.getScenes().stream())
                .filter(scene -> scene.getName().equals(basicConfigRepository.findFirstByOrderByModifiedDesc()
                        .orElseThrow()
                        .getSunsetSceneName()))
                .forEach(Scene::activate);
    }

    private boolean areAllLampsAreOff(Group group) {
        return deviceService.getDevicesOfType(SimpleLight.class)
                .stream()
                .filter(light -> group.getLights().contains(light.getId()))
                .noneMatch(SimpleLight::isCurrentStateIsOn);
    }
}
