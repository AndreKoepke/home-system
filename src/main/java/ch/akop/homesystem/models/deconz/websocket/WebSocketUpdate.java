package ch.akop.homesystem.models.deconz.websocket;

import lombok.Data;

import java.util.Map;

@Data
public class WebSocketUpdate {

    private String t;
    private String e;
    private String r;
    private String id;
    private String uniqueid;
    private String gid;
    private String scid;
    private Map<String, String> config;
    private String name;
    private String group;
    private Map<String, String> light;
    private Sensor sensor;
    private State state;
    private Attribute attr;

}
