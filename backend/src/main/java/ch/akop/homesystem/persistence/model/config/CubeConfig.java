package ch.akop.homesystem.persistence.model.config;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "config_cube")
@Getter
@Setter
public class CubeConfig {

  @Id
  private String name;

  private String sceneNameOnSide_1;
  private String sceneNameOnSide_2;
  private String sceneNameOnSide_3;
  private String sceneNameOnSide_4;
  private String sceneNameOnSide_5;
  private String sceneNameOnSide_6;
  private String deviceNameOnSide_1;
  private String deviceNameOnSide_2;
  private String deviceNameOnSide_3;
  private String deviceNameOnSide_4;
  private String deviceNameOnSide_5;
  private String deviceNameOnSide_6;
  private String sceneNameOnShake;

}
