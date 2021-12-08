package ch.akop.homesystem.models.deconz.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeconzLightResponse extends DeconzBaseDeviceResponse {

    private Integer colorcapabilities;
    private Integer ctmax;
    private Integer ctmin;


}
