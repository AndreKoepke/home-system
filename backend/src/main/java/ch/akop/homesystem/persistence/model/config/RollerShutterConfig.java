package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.persistence.conveter.ListOfEnumConverter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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
}
