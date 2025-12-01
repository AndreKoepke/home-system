package ch.akop.homesystem.persistence.model.animation.steps;

import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.persistence.conveter.ColorConverter;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.services.impl.DeviceService;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "animation_step_dimmer")
@Getter
@Setter
public class DimmLightStep implements Step {

  public static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);

  @Id
  @GeneratedValue
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

  @Override
  public String getActionDescription() {
    return "dimme " + nameOfLight + " zu " + dimmLightTo.multiply(BigDecimal.valueOf(100)) + "%";
  }

  @Nullable
  private Duration getDimmDuration() {
    return dimmDuration != null ? dimmDuration : DEFAULT_DURATION;
  }
}
