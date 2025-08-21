package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import java.time.ZonedDateTime;
import lombok.Data;

@Data
public class LightDto implements Identable {

  private String id;
  private String name;
  private int brightness;
  private Color color;
  private boolean on;
  private boolean reachable;
  private ZonedDateTime lastUpdated;

  public static LightDto from(SimpleLight light) {
    return new LightDto()
        .setId(light.getId())
        .setName(light.getName())
        .setReachable(light.isReachable())
        .setLastUpdated(light.getLastUpdated())
        .setOn(light.isCurrentStateIsOn());
  }

  public static LightDto from(DimmableLight light) {
    return from((SimpleLight) light)
        .setBrightness((int) (light.getCurrentBrightness() / 255f * 100));
  }

  public static LightDto from(ColoredLight light) {
    return from((DimmableLight) light)
        .setColor(light.getCurrentColor());
  }
}
