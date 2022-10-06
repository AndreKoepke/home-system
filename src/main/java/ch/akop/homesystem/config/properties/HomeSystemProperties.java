package ch.akop.homesystem.config.properties;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.models.animation.config.AnimationConfig;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Value
@ConfigurationProperties(prefix = "home-automation.special")
@Deprecated
public class HomeSystemProperties {

    @NonNull Double latitude;
    @NonNull Double longitude;
    @NonNull List<OffButton> centralOffSwitches;
    @Nullable String nightRunSceneName;
    @Nullable GoodNightButton goodNightButton;
    @Nullable String nightSceneName;
    @Nullable String nearestWeatherCloudStation;
    @Nullable String sunsetSceneName;
    @NonNull List<User> users;
    @NonNull List<MotionSensorConfig> motionSensors;
    boolean sendMessageWhenTurnLightsOff;
    @NonNull List<FanControlConfig> fans;
    @NonNull List<PowerMeterConfigs> powerMeters;
    @NonNull List<RollerShutterConfig> rollerShutters;
    @NonNull Set<String> notLights;
    @NonNull List<AnimationConfig> whenMainDoorOpened;
    @NonNull String mainDoorName;

    @Value
    public static class MotionSensorConfig {
        String sensor;
        List<String> lights;
        Duration keepMovingFor;
    }

    @Value
    public static class OffButton {
        @NonNull String name;
        @NonNull Integer buttonEvent;
    }

    @Value
    public static class FanControlConfig {
        @NonNull List<OffButton> buttons;
        @NonNull String fan;
        @Nullable String turnOffWhenLightTurnedOff;
        @Nullable String increaseTimeoutForMotionSensor;
    }

    @Value
    public static class PowerMeterConfigs {
        @NonNull String sensorName;
        @NonNull Integer isOnWhenMoreThan;
        @Nullable String messageWhenSwitchOn;
        @Nullable String messageWhenSwitchOff;
        @Nullable String linkToFan;
    }

    @Value
    public static class GoodNightButton {
        String name;
        Integer buttonEvent;
    }

    @Value
    @ToString(onlyExplicitlyIncluded = true)
    public static class User {

        @ToString.Include
        String name;

        String telegramId;
        String deviceIp;
        boolean dev;
    }

    @Value
    public static class RollerShutterConfig {
        @NonNull String name;
        @Nullable CompassDirection compassDirection;
        @Nullable LocalTime closeAt;
        @Nullable LocalTime openAt;
        boolean ignoreWeatherInTheMorning;
        boolean ignoreWeatherInTheEvening;
    }
}


