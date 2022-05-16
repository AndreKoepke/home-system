package ch.akop.homesystem.deconz.websocket;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class State {
    private Boolean all_on;
    private Boolean any_on;
    private Integer buttonevent;
    private String lastupdated;
    private Boolean open;
    private Boolean tampered;
    private Boolean presence;
    private Boolean dark;

    private Integer bri;
    private Boolean on;
    private Integer x;
    private List<BigDecimal> xy;
    private Integer y;

}
