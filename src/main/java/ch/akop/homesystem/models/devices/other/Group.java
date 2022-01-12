package ch.akop.homesystem.models.devices.other;

import ch.akop.homesystem.models.devices.Device;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class Group extends Device<Group> {

    @EqualsAndHashCode.Exclude
    private List<Scene> scenes;

}
