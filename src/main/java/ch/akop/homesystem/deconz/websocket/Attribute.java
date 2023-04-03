package ch.akop.homesystem.deconz.websocket;

import lombok.Data;

@Data
public class Attribute {

  private Integer colorcapabilities;
  private String id;
  private String lastannounced;
  private String lastseen;
  private String manufacturername;
  private String modelid;
  private String name;
  private String swversion;
  private String type;
  private String uniqueid;

}
