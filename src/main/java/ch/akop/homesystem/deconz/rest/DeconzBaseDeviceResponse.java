package ch.akop.homesystem.deconz.rest;

import lombok.Data;

@Data
public abstract class DeconzBaseDeviceResponse {

    private String etag;
    private boolean hascolor;
    private Object lastannounced;
    private String lastseen;
    private String manufacturername;
    private String modelid;
    private String name;
    private State state;
    private String swversion;
    private String type;
    private String uniqueid;

}
