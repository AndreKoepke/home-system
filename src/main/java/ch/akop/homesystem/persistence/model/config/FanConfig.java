package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "config_fan")
@Getter
@Setter
public class FanConfig {

    @Id
    private String name;

    @NonNull
    @Column(columnDefinition = "TEXT")
    private String triggerByButtonName;

    @NonNull
    private Integer triggerByButtonEvent;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String turnOffWhenLightTurnedOff;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String increaseTimeoutForMotionSensor;


}
