package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.conveter.ListOfStringsConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "config_timed_button")
@Getter
@Setter
public class TimedButtonConfig {

  @Id
  private String buttonName;

  private int buttonEvent;

  @NonNull
  private Duration keepOnFor;

  @NonNull
  @Column(columnDefinition = "TEXT", nullable = false)
  @Convert(converter = ListOfStringsConverter.class)
  private List<String> lights;

}
