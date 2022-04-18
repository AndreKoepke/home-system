package ch.akop.homesystem.message;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.devices.actor.Light;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.states.NormalState;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static ch.akop.weathercloud.light.LightUnit.WATT_PER_SQUARE_METER;

@Service
@RequiredArgsConstructor
public class SunsetReactor extends Activatable {

    private final WeatherService weatherService;
    private final MessageService messageService;
    private final DeviceService deviceService;
    private final HomeConfig homeConfig;

    private Weather previousWeather;

    @Override
    protected void started() {
        super.disposeWhenClosed(this.weatherService.getWeather()
                .doOnNext(this::turnLightsOnWhenItIsGettingDark)
                .doOnNext(weather -> this.previousWeather = weather)
                .subscribe());
    }

    private void turnLightsOnWhenItIsGettingDark(final Weather weather) {

        if (this.previousWeather == null
                || this.previousWeather.getLight().isSmallerThan(NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON, WATT_PER_SQUARE_METER)
                || weather.getLight().isBiggerThan(NormalState.THRESHOLD_NOT_TURN_LIGHTS_ON, WATT_PER_SQUARE_METER)) {
            return;
        }

        this.messageService.sendMessageToMainChannel("Es wird dunkel ... ich mach mal etwas Licht. Es sei denn ... /keinlicht");

        super.disposeWhenClosed(this.messageService.getMessages()
                .filter(message -> message.equalsIgnoreCase("/keinlicht"))
                .timeout(5, TimeUnit.MINUTES)
                .subscribe(s -> {}, this::activeSunsetScenes));
    }

    private void activeSunsetScenes(final Throwable ignored) {
        this.deviceService.getDevicesOfType(Group.class)
                .stream()
                .filter(this::areAllLampsAreOff)
                .flatMap(group -> group.getScenes().stream())
                .filter(scene -> scene.getName().equals(this.homeConfig.getSunsetSceneName()))
                .forEach(Scene::activate);
    }

    private boolean areAllLampsAreOff(final Group group) {
        return this.deviceService.getDevicesOfType(Light.class)
                .stream()
                .filter(light -> group.getLights().contains(light.getId()))
                .noneMatch(Light::isOn);
    }
}
