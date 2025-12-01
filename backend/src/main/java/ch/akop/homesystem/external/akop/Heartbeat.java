package ch.akop.homesystem.external.akop;

import java.util.UUID;
import lombok.Data;

@Data
public class Heartbeat {

  private UUID id;
  private String version;
}
