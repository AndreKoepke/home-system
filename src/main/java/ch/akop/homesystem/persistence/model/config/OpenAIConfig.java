package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.external.openai.ImageRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;


@Entity
@Table(name = "config_openai")
@Getter
@Setter
public class OpenAIConfig {

    @Id
    @LastModifiedDate
    private LocalDateTime modified = LocalDateTime.now();

    @NonNull
    private String apiKey;

    @NonNull
    private ImageRequest.Size size;
}
