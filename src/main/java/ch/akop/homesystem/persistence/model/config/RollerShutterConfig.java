package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.models.CompassDirection;
import java.time.LocalTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "config_roller_shutter")
@Getter
@Setter
public class RollerShutterConfig {

  @Id
  @Column(columnDefinition = "TEXT")
  private String name;

  @Nullable
  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "TEXT")
  private CompassDirection compassDirection;

  @Nullable
  private LocalTime closeAt;

  @Nullable
  private LocalTime openAt;

  private boolean ignoreWeatherInTheMorning;
  private boolean ignoreWeatherInTheEvening;

}
