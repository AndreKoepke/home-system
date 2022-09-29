package ch.akop.homesystem.persistence.model.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.Nullable;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "config_basic")
@Getter
@Setter
public class BasicConfig {

    @Id
    @LastModifiedDate
    private LocalDateTime modified = LocalDateTime.now();

    @Nullable
    private Double latitude;

    @Nullable
    private Double longitude;

    @Nullable
    private String nightSceneName;

    @Nullable
    private String nearestWeatherCloudStation;

    @Nullable
    private String sunsetSceneName;

    private boolean sendMessageWhenTurnLightsOff;

    @ElementCollection
    private Set<String> notLights;

    @Nullable
    private String goodNightButtonName;

    @Nullable
    private Integer goodNightButtonEvent;

}
