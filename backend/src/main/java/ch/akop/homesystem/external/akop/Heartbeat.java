package ch.akop.homesystem.external.akop;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class Heartbeat {

  private UUID id;
  private String gitBranch;
  private String gitCommit;
  private LocalDateTime gitCommitDate;
}
