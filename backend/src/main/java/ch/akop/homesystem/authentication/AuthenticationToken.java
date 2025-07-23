package ch.akop.homesystem.authentication;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "config_auth_tokens")
@Getter
@Setter
class AuthenticationToken {

  @Id
  private UUID id;

  @CreationTimestamp
  private LocalDateTime created = LocalDateTime.now();

  private LocalDateTime lastTimeUsed = LocalDateTime.now();
  private String description;
  private String token;
}
