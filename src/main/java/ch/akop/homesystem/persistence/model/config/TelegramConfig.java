package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "config_telegram")
@Getter
@Setter
public class TelegramConfig {

    @Id
    @LastModifiedDate
    private LocalDateTime modified = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String botToken;

    @Column(columnDefinition = "TEXT")
    private String botUsername;

    @Column(columnDefinition = "TEXT")
    private String botPath;

    @Column(columnDefinition = "TEXT")
    private String mainChannel;
}
