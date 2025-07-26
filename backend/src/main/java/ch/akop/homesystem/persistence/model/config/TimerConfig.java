package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.model.animation.Animation;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.Nullable;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "config_timer")
@Getter
@Setter
public class TimerController {

    @Id
    private UUID id;

  @Type(ListArrayType.class)
  @Column(name = "sensor_names", columnDefinition = "text[]")
    private List<String> lights;

}
