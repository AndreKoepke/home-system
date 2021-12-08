package ch.akop.homesystem.models.deconz.rest;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateLightParameters {

    private String alert;

    @Max(255)
    @Min(0)
    private Integer bri;

    @Max(255)
    @Min(1)
    private Integer colorloopspeed;

    private  Integer ct;

    private String effect;

    @Max(65535)
    @Min(0)
    private Integer hue;

    private Boolean on;

    private Integer transitiontime;

    @Size(min = 2, max = 2)
    private List<BigDecimal> xy;

}
