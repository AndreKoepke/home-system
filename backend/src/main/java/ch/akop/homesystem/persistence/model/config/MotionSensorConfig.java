package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.model.animation.Animation;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "config_motion_sensor")
@Getter
@Setter
public class MotionSensorConfig {

  @Id
  @Column(columnDefinition = "TEXT")
  private String name;

  @NonNull
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "config_motion_sensor_lights")
  @MapKeyColumn(columnDefinition = "TEXT")
  @Column(columnDefinition = "TEXT")
  private List<String> lights;

  @NonNull
  private Duration keepMovingFor;

  @Nullable
  private Integer onlyTurnOnWhenDarkerAs;

  @Nullable
  private Boolean onlyAtNormalState;

  private LocalTime notBefore;

  @OneToOne
  @Nullable
  private Animation animation;

}
