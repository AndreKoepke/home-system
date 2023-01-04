package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

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
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "config_motion_sensor_lights")
    @MapKeyColumn(columnDefinition = "TEXT")
    @Column(columnDefinition = "TEXT")
    private List<String> lights;

    @NonNull
    private Duration keepMovingFor;

}
