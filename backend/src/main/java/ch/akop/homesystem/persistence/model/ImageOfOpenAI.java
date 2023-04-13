package ch.akop.homesystem.persistence.model;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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
