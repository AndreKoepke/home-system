package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "config_telegram")
@Getter
@Setter
public class TelegramConfig {

  @Id
  private LocalDateTime modified = LocalDateTime.now();

  @Column(columnDefinition = "TEXT")
  private String botToken;

  @Column(columnDefinition = "TEXT")
  private String botPath;

  @Column(columnDefinition = "TEXT")
  private String mainChannel;
}
