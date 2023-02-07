package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.external.openai.ImageRequest;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;


@Entity
@Table(name = "config_openai")
@Getter
@Setter
public class OpenAIConfig {

    @Id
    @UpdateTimestamp
    private LocalDateTime modified = LocalDateTime.now();

    @NonNull
    private String apiKey;

    @NonNull
    private ImageRequest.Size size;
}
