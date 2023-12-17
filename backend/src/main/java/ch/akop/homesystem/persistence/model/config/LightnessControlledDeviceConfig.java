package ch.akop.homesystem.persistence.model.config;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

import ch.akop.weathercloud.light.Light;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "config_lightness_controlled_device")
@Getter
@Setter
public class LightnessControlledDeviceConfig {

  /**
   * Same like the device name.
   */
  @Id
  private String name;

  @Nullable
  private Integer turnOnWhenDarkerAs;

  @Nullable
  private Integer turnOffWhenLighterAs;


  public boolean isDarkerAs(Light light) {
    if (turnOnWhenDarkerAs == null) {
      return false;
    }

    return light.isSmallerThan(turnOnWhenDarkerAs, KILO_LUX);
  }

  public boolean isLighterAs(Light light) {
    if (turnOffWhenLighterAs == null) {
      return false;
    }

    return light.isSmallerThan(turnOffWhenLighterAs, KILO_LUX);
  }
}
