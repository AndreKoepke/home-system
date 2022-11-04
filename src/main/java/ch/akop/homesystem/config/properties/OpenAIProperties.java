package ch.akop.homesystem.config.properties;

import ch.akop.homesystem.openai.ImageRequest;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Value
@ConstructorBinding
@ConfigurationProperties(prefix = "home-automation.openai")
public class OpenAIProperties {
    String apiKey;
    ImageRequest.Size size;
}
