package ch.akop.homesystem.models.devices.other;

import ch.akop.homesystem.models.devices.Device;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public class Scene extends Device<Scene> {

    @ToString.Exclude
    private final Group parent;
    private final Runnable activateSceneRunnable;


    public void activate() {
        this.activateSceneRunnable.run();
    }

}
