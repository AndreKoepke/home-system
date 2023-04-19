package ch.akop.homesystem.persistence.model.config;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Table(name = "config_deconz")
@Entity
public class DeconzConfig {

  @Id
  @UpdateTimestamp
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
