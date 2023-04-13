package ch.akop.homesystem.deconz.rest.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Light extends BaseDevice {

  private Integer colorcapabilities;
  private Integer ctmax;
  private Integer ctmin;


}
