package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.conveter.ListOfStringsConverter;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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


  public boolean eventMatches(String buttonName, int buttonEvent) {
    return Objects.equals(this.buttonName, buttonName) && Objects.equals(this.buttonEvent, buttonEvent);
  }
}
