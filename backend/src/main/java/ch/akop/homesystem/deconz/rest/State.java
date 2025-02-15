package ch.akop.homesystem.deconz.rest;

import java.math.BigDecimal;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import lombok.Data;

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

  private Integer current;
  private Integer power;
  private Integer voltage;

  private Integer buttonevent;
  private Boolean presence;
  private Integer targetdistance;
  private Boolean dark;

  private Integer lightlevel;
  private Integer lux;
  private Boolean daylight;

  private String lastupdated;
}
