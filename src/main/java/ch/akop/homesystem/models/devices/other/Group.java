package ch.akop.homesystem.models.devices.other;

import ch.akop.homesystem.deconz.rest.State;
import ch.akop.homesystem.models.devices.Device;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Group extends Device<Group> {

  @EqualsAndHashCode.Exclude
  private List<Scene> scenes;

  private List<String> lights;

  @Override
  protected void consumeInternalUpdate(State update) {
    // NOP
  }
}
