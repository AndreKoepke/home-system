package ch.akop.homesystem.telemetry;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TelemetryData {

  @Id
  public UUID id;
}
