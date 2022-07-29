package ch.akop.homesystem.models.devices.actor;

import ch.akop.homesystem.models.devices.Device;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RollerShutter extends Device<RollerShutter> {

    private final Consumer<Integer> functionToSetLift;
    private final Consumer<Integer> functionToSetTilt;
    private final Runnable functionToStep;

    /**
     * 100% means, it is open
     * 0% means, it is closed
     */
    @Min(0)
    @Max(100)
    private Integer lift;

    /**
     * Tilt angle
     */
    @Min(0)
    @Max(100)
    private Integer tilt;

}
