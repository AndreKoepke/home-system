package ch.akop.homesystem.config.properties;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Value
@ConstructorBinding
@ConfigurationProperties(prefix = "home-automation.mastodon")
public class MastodonProperties {
    String server;
    String token;
}
