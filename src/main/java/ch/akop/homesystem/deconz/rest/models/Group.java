package ch.akop.homesystem.deconz.rest.models;

import java.util.List;
import lombok.Data;

@Data
public class Group {

  private String name;
  private String id;
  private String type;
  private List<Scene> scenes;
  private List<String> lights;
}
