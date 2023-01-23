package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.persistence.model.animation.Animation;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "config_basic")
@Getter
@Setter
public class BasicConfig {

    @Id
    @UpdateTimestamp
    private LocalDateTime modified = LocalDateTime.now();

    @Nullable
    private Double latitude;

    @Nullable
    private Double longitude;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String nightSceneName;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String nearestWeatherCloudStation;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String sunsetSceneName;

    private boolean sendMessageWhenTurnLightsOff;

    @ElementCollection
    @CollectionTable(name = "config_basic_not_lights")
    @MapKeyColumn(columnDefinition = "TEXT")
    @Column(columnDefinition = "TEXT")
    private Set<String> notLights;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String goodNightButtonName;

    @Nullable
    private Integer goodNightButtonEvent;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String nightRunSceneName;

    @Nullable
    private String mainDoorName;

    @ManyToOne
    private Animation whenMainDoorOpened;
}
