package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.persistence.conveter.ColorConverter;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.impl.DeviceService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "animation_step_dimmer")
@Getter
@Setter
public class DimmLightStep implements Step {

  public static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @NonNull
  private Integer sortOrder;

  @ManyToOne
  private Animation animation;

  @NonNull
  private String nameOfLight;

  @NonNull
  private BigDecimal dimmLightTo;

  @Nullable
  @Column(name = "dimm_duration")
  private Duration dimmDuration;

  @Nullable
  @Convert(converter = ColorConverter.class)
  private Color color;


  @Override
  public void play(DeviceService deviceService) {
    if (color != null) {
      deviceService.findDeviceByName(nameOfLight, ColoredLight.class)
          .orElseThrow(() -> new EntityNotFoundException("Colored light with name " + nameOfLight + " was not found"))
          .setColorAndBrightness(color, dimmDuration, dimmLightTo);
    } else {
      var light = deviceService.findDeviceByName(nameOfLight, DimmableLight.class)
          .orElseThrow(() -> new EntityNotFoundException("Light with name " + nameOfLight + " was not found"));
      light.setBrightness(dimmLightTo, getDimmDuration());
    }
  }

  @Nullable
  private Duration getDimmDuration() {
    return dimmDuration != null ? dimmDuration : DEFAULT_DURATION;
  }
}
