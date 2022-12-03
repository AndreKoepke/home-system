package ch.akop.homesystem.config.properties;

import ch.akop.homesystem.external.openai.ImageRequest;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Value
@ConfigurationProperties(prefix = "home-automation.openai")
public class OpenAIProperties {
    String apiKey;
    ImageRequest.Size size;
}
