package ch.akop.homesystem.deconz.websocket;

import lombok.Data;

import java.util.Map;

@Data
public class Sensor {

    private Map<String, String> config;
    private int ep;
    private String etag;
    private String id;
    private String manufacturername;
    private int mode;
    private String modelid;
    private String name;
    private Map<String, String> state;
    private String type;
    private String uniqueid;

}
