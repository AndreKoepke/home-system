package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.models.CompassDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.time.LocalTime;

@Entity
@Table(name = "config_roller_shutter")
@Getter
@Setter
public class RollerShutterConfig {

    @Id
    @Column(columnDefinition = "TEXT")
    private String name;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TEXT")
    private CompassDirection compassDirection;

    @Nullable
    private LocalTime closeAt;

    @Nullable
    private LocalTime openAt;

    private boolean ignoreWeatherInTheMorning;
    private boolean ignoreWeatherInTheEvening;

}
