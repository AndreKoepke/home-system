package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import ch.akop.homesystem.persistence.model.config.*;
import ch.akop.homesystem.persistence.repository.config.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class ConfigTransformator {

    private final HomeSystemProperties homeSystemProperties;
    private final ch.akop.homesystem.deconz.DeconzConfig deconzConfigOld;
    private final TelegramMessageService telegramMessageService;

    private final BasicConfigRepository basicConfigRepository;
    private final DeconzConfigRepository deconzConfigRepository;
    private final FanConfigRepository fanConfigRepository;
    private final MotionSensorConfigRepository motionSensorConfigRepository;
    private final OffButtonConfigRepository offButtonConfigRepository;
    private final PowerMeterConfigRepository powerMeterConfigRepository;
    private final RollerShutterConfigRepository rollerShutterConfigRepository;
    private final UserConfigRepository userConfigRepository;
    private final TelegramConfigRepository telegramConfigRepository;

    @PostConstruct
    public void copyLocalSettingsToDatabase() {
        basicConfigRepository.save(new BasicConfig()
                .setLatitude(homeSystemProperties.getLatitude())
                .setLongitude(homeSystemProperties.getLongitude())
                .setNightSceneName(homeSystemProperties.getNightSceneName())
                .setNearestWeatherCloudStation(homeSystemProperties.getNearestWeatherCloudStation())
                .setSunsetSceneName(homeSystemProperties.getSunsetSceneName())
                .setSendMessageWhenTurnLightsOff(homeSystemProperties.isSendMessageWhenTurnLightsOff())
                .setNotLights(homeSystemProperties.getNotLights())
                .setGoodNightButtonName(homeSystemProperties.getGoodNightButton() != null ? homeSystemProperties.getGoodNightButton().getName() : null)
                .setGoodNightButtonEvent(homeSystemProperties.getGoodNightButton() != null ? homeSystemProperties.getGoodNightButton().getButtonEvent() : null));

        deconzConfigRepository.save(new DeconzConfig()
                .setHost(deconzConfigOld.getHost())
                .setPort(deconzConfigOld.getPort())
                .setApiKey(deconzConfigOld.getApiKey())
                .setWebsocketPort(deconzConfigOld.getWebsocketPort()));

        homeSystemProperties.getFans()
                .forEach(fanConfig -> fanConfigRepository.save(new FanConfig()
                        .setName(fanConfig.getFan())
                        .setTurnOffWhenLightTurnedOff(fanConfig.getTurnOffWhenLightTurnedOff())
                        .setTriggerByButtonName(fanConfig.getButtons().get(0).getName())
                        .setTriggerByButtonEvent(fanConfig.getButtons().get(0).getButtonEvent())
                        .setIncreaseTimeoutForMotionSensor(fanConfig.getIncreaseTimeoutForMotionSensor())));

        homeSystemProperties.getMotionSensors()
                .forEach(motionConfig -> motionSensorConfigRepository.save(new MotionSensorConfig()
                        .setName(motionConfig.getSensor())
                        .setLights(motionConfig.getLights())
                        .setKeepMovingFor(motionConfig.getKeepMovingFor())));

        homeSystemProperties.getCentralOffSwitches()
                .forEach(offButton -> offButtonConfigRepository.save(new OffButtonConfig()
                        .setName(offButton.getName())
                        .setButtonEvent(offButton.getButtonEvent())));

        homeSystemProperties.getPowerMeters()
                .forEach(powerMeterConfig -> powerMeterConfigRepository.save(new PowerMeterConfig()
                        .setName(powerMeterConfig.getSensorName())
                        .setLinkedFan(powerMeterConfig.getLinkToFan() != null ? fanConfigRepository.getReferenceById(powerMeterConfig.getLinkToFan()) : null)
                        .setIsOnWhenMoreThan(powerMeterConfig.getIsOnWhenMoreThan())
                        .setMessageWhenSwitchOn(powerMeterConfig.getMessageWhenSwitchOn())
                        .setMessageWhenSwitchOff(powerMeterConfig.getMessageWhenSwitchOff())));

        homeSystemProperties.getRollerShutters()
                .forEach(rollerShutterConfig -> rollerShutterConfigRepository.save(new RollerShutterConfig()
                        .setName(rollerShutterConfig.getName())
                        .setOpenAt(rollerShutterConfig.getOpenAt())
                        .setCloseAt(rollerShutterConfig.getCloseAt())
                        .setCompassDirection(rollerShutterConfig.getCompassDirection())));

        homeSystemProperties.getUsers()
                .forEach(user -> userConfigRepository.save(new UserConfig()
                        .setName(user.getName())
                        .setDeviceIp(user.getDeviceIp())
                        .setTelegramId(user.getTelegramId())
                        .setDev(user.isDev())));

        telegramConfigRepository.save(new TelegramConfig()
                .setBotUsername(telegramMessageService.getBotUsername())
                .setBotToken(telegramMessageService.getBotToken())
                .setMainChannel(telegramMessageService.getMainChannel())
                .setBotPath(telegramMessageService.getBotPath()));
    }

}
