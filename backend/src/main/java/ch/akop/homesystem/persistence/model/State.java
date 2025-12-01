package ch.akop.homesystem.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "state")
@Getter
@Setter
public class State {

  @Id
  @Column(name = "activated_at", nullable = false)
  private LocalDateTime activatedAt = LocalDateTime.now();

  @NonNull
  @Column(name = "class_name", nullable = false)
  private String className;
}
