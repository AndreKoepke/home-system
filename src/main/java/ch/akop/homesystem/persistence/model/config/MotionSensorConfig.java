package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Duration;
import java.util.List;

@Entity
@Table(name = "config_motion_sensor")
@Getter
@Setter
public class MotionSensorConfig {

    @Id
    private String name;

    @ElementCollection
    @NonNull
    private List<String> lights;

    @NonNull
    private Duration keepMovingFor;

}
