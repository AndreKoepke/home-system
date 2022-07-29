package ch.akop.homesystem.deconz.rest;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Data
public class State {
    private Boolean reachable;
    private String alert;

    @Max(255)
    @Min(0)
    private Integer bri;
    private String colormode;
    private Integer ct;
    private Boolean on;
    private String effect;

    @Max(65535)
    @Min(0)
    private Integer hue;

    @Max(255)
    @Min(0)
    private Integer sat;

    @Size(min = 2, max = 2)
    private List<BigDecimal> xy;

    private Integer lift;
    private Integer tilt;
    private Boolean open;
    private Integer transitiontime;

    @Max(255)
    @Min(1)
    private Integer colorspeed;

    private Boolean stop;
}
