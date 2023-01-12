package ch.akop.homesystem.persistence.model.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

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

    @NonNull
    private Boolean turnOffWhenReady;

    @NonNull
    private Boolean gotReadyWhenNobodyWasHome;

    @NonNull
    private Integer sendRemindersCount;

}
