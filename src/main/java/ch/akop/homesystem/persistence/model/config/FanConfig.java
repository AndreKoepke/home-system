package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "config_fan")
@Getter
@Setter
public class FanConfig {

    @Id
    private String name;

    @NonNull
    private String triggerByButtonName;

    @NonNull
    private Integer triggerByButtonEvent;

    @Nullable
    private String turnOffWhenLightTurnedOff;

    @Nullable
    private String increaseTimeoutForMotionSensor;


}
