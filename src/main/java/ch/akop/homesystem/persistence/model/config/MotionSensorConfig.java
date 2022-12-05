package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.*;
import java.time.Duration;
import java.util.List;

@Entity
@Table(name = "config_motion_sensor")
@Getter
@Setter
public class MotionSensorConfig {

    @Id
    @Column(columnDefinition = "TEXT")
    private String name;

    @NonNull
    @ElementCollection
    @CollectionTable(name = "config_motion_sensor_lights")
    @MapKeyColumn(columnDefinition = "TEXT")
    @Column(columnDefinition = "TEXT")
    private List<String> lights;

    @NonNull
    private Duration keepMovingFor;

}
