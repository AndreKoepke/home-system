package ch.akop.homesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@ConfigurationProperties(prefix = "home-automation.special")
@Service
@Data
public class HomeConfig {

    private List<String> nightLights;
    private List<OffButton> centralOffSwitches;


    @Data
    public static class OffButton {

        private String name;
        private int buttonEvent;

    }

}


