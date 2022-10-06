package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;

@Entity
@Table(name = "config_power_meter")
@Getter
@Setter
public class PowerMeterConfig {

    @Id
    @Column(columnDefinition = "TEXT")
    private String name;

    @NonNull
    private Integer isOnWhenMoreThan;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String messageWhenSwitchOn;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String messageWhenSwitchOff;

    @ManyToOne
    @Nullable
    private FanConfig linkedFan;

}
