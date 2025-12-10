package ch.akop.homesystem.persistence.model.config;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static java.time.LocalTime.now;

import ch.akop.weathercloud.light.Light;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
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

  @Nullable
  private LocalTime keepOffFrom;

  @Nullable
  private LocalTime keepOffTo;


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

    return light.isBiggerThan(turnOffWhenLighterAs, KILO_LUX);
  }

  public boolean isTimeOkForBeingOn() {
    if (keepOffFrom == null || keepOffTo == null) {
      return true;
    }

    if (keepOffFrom.isAfter(keepOffTo)) {
      return checkAgainstNegativeTimeWindow(keepOffFrom, keepOffTo);
    } else {
      return checkAgainstPositiveTimeWindow(keepOffFrom, keepOffTo);
    }
  }

  /**
   * Example: from 7:00 to 20:00, the lights should be off
   *
   * @return true, when the lights can be on
   */
  private boolean checkAgainstPositiveTimeWindow(LocalTime keepOffFrom, LocalTime keepOffTo) {
    return now().isBefore(keepOffFrom) || now().isAfter(keepOffTo);
  }

  /**
   * Example: from 23:00 to 05:00 the lights should be off
   * @return true, when lights can be on
   */
  private static boolean checkAgainstNegativeTimeWindow(LocalTime keepOffFrom, LocalTime keepOffTo) {
    return !now().isAfter(keepOffFrom) && !now().isBefore(keepOffTo);
  }
}
