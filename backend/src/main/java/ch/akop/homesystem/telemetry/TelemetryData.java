package ch.akop.homesystem.telemetry;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TelemetryData {

  @Id
  public UUID id;
}
