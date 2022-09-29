package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "config_telegram")
@Getter
@Setter
public class TelegramConfig {

    @Id
    @LastModifiedDate
    private LocalDateTime modified = LocalDateTime.now();

    private String botToken;
    private String botUsername;
    private String botPath;
    private String mainChannel;
}
