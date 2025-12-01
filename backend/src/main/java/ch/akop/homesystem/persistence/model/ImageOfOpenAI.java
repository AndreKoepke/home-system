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
@Table(name = "openai_images")
@Getter
@Setter
public class ImageOfOpenAI {

  @Id
  private LocalDateTime created = LocalDateTime.now();

  @Column(nullable = false)
  private int downloaded = 0;

  @Column(nullable = false)
  @NonNull
  private String prompt;

  private byte[] image;
}
