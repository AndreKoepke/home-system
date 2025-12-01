package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.persistence.conveter.ListOfEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
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

  @NonNull
  @Column(columnDefinition = "TEXT")
  @Convert(converter = ListOfEnumConverter.class)
  private List<CompassDirection> compassDirection;

  @Nullable
  private LocalTime closeAt;

  @Nullable
  private LocalTime openAt;

  @Nullable
  private LocalDateTime noAutomaticsUntil;

  @Nullable
  private Boolean closeWithInterrupt;

  private int highSunLevel;
  private int closeLevelLowTilt;
  private int closeLevelHighTilt;
  private int closeLevelLowLift;
  private int closeLevelHighLift;
}
