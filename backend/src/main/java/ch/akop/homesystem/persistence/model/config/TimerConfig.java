package ch.akop.homesystem.persistence.model.config;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "config_timer")
@Getter
@Setter
public class TimerConfig {

  @Id
  private UUID id;

  private String name;

  @Nullable
  private LocalTime turnOnAt;

  @Nullable
  private LocalTime turnOffAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "config_timer_to_device",
      joinColumns = @JoinColumn(name = "timer_id")
  )
  @Column(name = "device_name")
  private List<String> devices = new ArrayList<>();


}
