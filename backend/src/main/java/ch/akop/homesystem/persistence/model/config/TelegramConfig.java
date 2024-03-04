package ch.akop.homesystem.persistence.model.config;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "config_telegram")
@Getter
@Setter
public class TelegramConfig {

  @Id
  @UpdateTimestamp
  private LocalDateTime modified = LocalDateTime.now();

  @Column(columnDefinition = "TEXT")
  private String botToken;

  @Column(columnDefinition = "TEXT")
  private String botPath;

  @Column(columnDefinition = "TEXT")
  private String mainChannel;
}
