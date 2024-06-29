package ch.akop.homesystem.persistence.model.config;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

import ch.akop.weathercloud.light.Light;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    var now = LocalDateTime.now();

    return !(now.isAfter(getKeepOffFromAsMidnightAware())
        && now.isBefore(keepOffTo.atDate(LocalDate.now())));
  }


  /**
   * Precondition: Only call when {@link LightnessControlledDeviceConfig#getKeepOffFrom()} and {@link  LightnessControlledDeviceConfig#getKeepOffTo()} are not null.
   */
  private LocalDateTime getKeepOffFromAsMidnightAware() {
    //noinspection DataFlowIssue
    if (keepOffFrom.isAfter(keepOffTo) && keepOffFrom.isAfter(LocalTime.now())) {
      return keepOffFrom.atDate(LocalDate.now().minusDays(1));
    }
    return keepOffFrom.atDate(LocalDate.now());
  }
}
