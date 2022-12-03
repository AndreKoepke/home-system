package ch.akop.homesystem.config.properties;

import ch.akop.homesystem.external.openai.ImageRequest;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "home-automation.openai")
public class OpenAIProperties {
    String apiKey;
    ImageRequest.Size size;
}
