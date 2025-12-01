package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
