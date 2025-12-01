package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Table(name = "config_deconz")
@Entity
public class DeconzConfig {

  @Id
  private LocalDateTime modified = LocalDateTime.now();

  @NonNull
  @Column(columnDefinition = "TEXT")
  private String host;

  @NonNull
  @Column(columnDefinition = "TEXT")
  private String apiKey;

  private int port;
  private int websocketPort;

}
