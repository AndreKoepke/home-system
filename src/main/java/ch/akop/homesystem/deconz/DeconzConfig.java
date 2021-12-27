package ch.akop.homesystem.deconz;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "home-automation.deconz")
@Component
public class DeconzConfig {

    private String host;
    private String apiKey;
    private int port;
    private int websocketPort;

}
