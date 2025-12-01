package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.model.animation.Animation;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "config_motion_sensor_lights",
      joinColumns = @JoinColumn(name = "motion_sensor_config_name"))
  @Column(name = "lights")
  private Set<String> lights;

  @NonNull
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "config_motion_sensor_lights_night",
      joinColumns = @JoinColumn(name = "motion_sensor_config_name"))
  @Column(name = "lights")
  private Set<String> lightsAtNight;

  @Nullable
  private Duration keepMovingFor;

  @Nullable
  private Integer onlyTurnOnWhenDarkerAs;

  @Nullable
  private Boolean onlyAtNormalState;

  @Nullable
  private Integer selfLightNoise;

  private boolean turnLightOnWhenMovement;

  private LocalTime notBefore;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "animation_id", referencedColumnName = "id")
  @Nullable
  private Animation animation;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "animation_id_night", referencedColumnName = "id")
  @Nullable
  private Animation animationNight;

  public Set<String> getAffectedLightNames(boolean nightState) {
    if (nightState && (!getLightsAtNight().isEmpty() || getAnimationNight() != null)) {
      var allRelatedLights = new HashSet<>(getLightsAtNight());
      Optional.ofNullable(getAnimationNight()).ifPresent(animation -> allRelatedLights.addAll(animation.getLights()));
      return allRelatedLights;
    }
    var allRelatedLights = new HashSet<>(getLights());
    Optional.ofNullable(getAnimation()).ifPresent(animation -> allRelatedLights.addAll(animation.getLights()));
    return allRelatedLights;
  }
}
