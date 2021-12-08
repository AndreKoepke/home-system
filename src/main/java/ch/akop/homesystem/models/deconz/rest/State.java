package ch.akop.homesystem.models.deconz.rest;

import lombok.Data;

import java.util.List;

@Data
public class State {
    private boolean reachable;
    private String alert;
    private int bri;
    private String colormode;
    private int ct;
    private boolean on;
    private String effect;
    private int hue;
    private int sat;
    private List<Double> xy;
    private int lift;
    private boolean open;
}
