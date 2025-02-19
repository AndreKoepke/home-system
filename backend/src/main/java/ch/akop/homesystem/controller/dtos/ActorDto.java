package ch.akop.homesystem.controller.dtos;

import ch.akop.homesystem.models.color.Color;
import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.ColoredLight;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import lombok.Data;

@Data
public class ActorDto {

  private String id;
  private String name;
  private int brightness;
  private Color color;
  private boolean on;
  private boolean reachable;


  public static ActorDto from(Device<?> light) {
    return new ActorDto()
        .setId(light.getId())
        .setName(light.getName())
        .setReachable(light.isReachable());
  }

  public static ActorDto from(SimpleLight light) {
    return from((Device<?>) light)
        .setOn(light.isCurrentStateIsOn());
  }

  public static ActorDto from(DimmableLight light) {
    return from((SimpleLight) light)
        .setBrightness((int) (light.getCurrentBrightness() / 255f * 100));
  }

  public static ActorDto from(ColoredLight light) {
    return from((DimmableLight) light)
        .setColor(light.getCurrentColor());
  }
}
