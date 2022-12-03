package ch.akop.homesystem.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "home-automation.mastodon")
public class MastodonProperties {
    String server;
    String token;
}
