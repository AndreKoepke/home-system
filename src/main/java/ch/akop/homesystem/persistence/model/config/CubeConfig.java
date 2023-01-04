package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private String sceneNameOnShake;

}
