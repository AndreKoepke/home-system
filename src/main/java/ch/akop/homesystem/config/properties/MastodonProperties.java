package ch.akop.homesystem.config.properties;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Value
@ConfigurationProperties(prefix = "home-automation.mastodon")
public class MastodonProperties {
    String server;
    String token;
}
