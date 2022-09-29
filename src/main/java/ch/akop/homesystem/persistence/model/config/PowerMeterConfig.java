package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "config_power_meter")
@Getter
@Setter
public class PowerMeterConfig {

    @Id
    private String name;

    @NonNull
    private Integer isOnWhenMoreThan;

    @Nullable
    private String messageWhenSwitchOn;

    @Nullable
    private String messageWhenSwitchOff;

    @ManyToOne
    @Nullable
    private FanConfig linkedFan;

}
