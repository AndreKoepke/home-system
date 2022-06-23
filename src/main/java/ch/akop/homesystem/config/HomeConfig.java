package ch.akop.homesystem.config;

import ch.akop.homesystem.models.config.GoodNightButton;
import ch.akop.homesystem.models.config.User;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ConfigurationProperties(prefix = "home-automation.special")
@Service
@Data
public class HomeConfig {

    private List<OffButton> centralOffSwitches;
    private String nightRunSceneName;
    private GoodNightButton goodNightButton;
    private String nightSceneName;
    private String nearestWeatherCloudStation;
    private String sunsetSceneName;
    private List<User> users;
    private List<MotionSensorConfig> motionSensors;
    private boolean sendMessageWhenTurnLightsOff = true;

    @Data
    public static class MotionSensorConfig {
        private String sensor;
        private List<String> lights;
        private boolean ignoreBrightness;
        private Duration keepMovingFor = Duration.of(5, ChronoUnit.MINUTES);
    }

    @Data
    public static class OffButton {
        private String name;
        private int buttonEvent;
    }

}


