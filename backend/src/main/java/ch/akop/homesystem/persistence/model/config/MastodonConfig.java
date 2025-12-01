package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "config_mastodon")
@Getter
@Setter
public class MastodonConfig {

  @Id
  private LocalDateTime modified = LocalDateTime.now();

  @NonNull
  private String server;

  @NonNull
  private String token;
}
