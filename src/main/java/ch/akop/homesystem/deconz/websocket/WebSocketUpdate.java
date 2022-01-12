package ch.akop.homesystem.deconz.websocket;

import lombok.Data;

@Data
public class WebSocketUpdate {

    private String t;
    private String e;
    private String r;
    private String id;
    private String uniqueid;
    private String gid;
    private String scid;

    // "config" can contain a list, so Map<object,object> not fit
    // "light" can contain objects, so Map<String,String> not fit

    private String name;
    private String group;
    private Sensor sensor;
    private State state;
    private Attribute attr;

}
