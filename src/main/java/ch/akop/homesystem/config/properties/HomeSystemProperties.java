package ch.akop.homesystem.config.properties;

import ch.akop.homesystem.models.CompassDirection;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.List;

@Value
@ConstructorBinding
@ConfigurationProperties(prefix = "home-automation.special")
public class HomeSystemProperties {

    @NonNull Double latitude;
    @NonNull Double longitude;

    @NonNull List<OffButton> centralOffSwitches;
    @Nullable
    String nightRunSceneName;
    @Nullable
    GoodNightButton goodNightButton;
    @Nullable
    String nightSceneName;
    @Nullable
    String nearestWeatherCloudStation;
    @Nullable
    String sunsetSceneName;
    @NonNull List<User> users;
    @NonNull List<MotionSensorConfig> motionSensors;
    boolean sendMessageWhenTurnLightsOff;
    @NonNull List<FanControlConfig> fans;
    @NonNull List<PowerMeterConfigs> powerMeters;
    @NonNull List<RollerShutter> rollerShutters;

    @Value
    @ConstructorBinding
    public static class MotionSensorConfig {
        String sensor;
        List<String> lights;
        Duration keepMovingFor;
    }

    @Value
    @ConstructorBinding
    public static class OffButton {
        @NonNull String name;
        @NonNull Integer buttonEvent;
    }

    @Value
    @ConstructorBinding
    public static class FanControlConfig {
        @NonNull List<OffButton> buttons;
        @NonNull String fan;
        @Nullable
        String turnOffWhenLightTurnedOff;
        @Nullable
        String increaseTimeoutForMotionSensor;
        @Nullable
        String increaseMotionSensorTimeout;
    }

    @Value
    @ConstructorBinding
    public static class PowerMeterConfigs {
        @NonNull String sensorName;
        @NonNull Integer isOnWhenMoreThan;
        @Nullable
        String messageWhenSwitchOn;
        @Nullable
        String messageWhenSwitchOff;
        @Nullable
        String linkToFan;
    }

    @Value
    @ConstructorBinding
    public static class GoodNightButton {
        String name;
        Integer buttonEvent;
    }

    @Value
    @ConstructorBinding
    @ToString(onlyExplicitlyIncluded = true)
    public static class User {

        @ToString.Include
        String name;

        String telegramId;
        String deviceIp;
        boolean dev;
    }

    @Value
    @ConstructorBinding
    public static class RollerShutter {
        @NonNull String name;
        @NonNull CompassDirection compassDirection;
    }
}


