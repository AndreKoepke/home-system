package ch.akop.homesystem.persistence.model.config;

import ch.akop.homesystem.models.CompassDirection;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "config_roller_shutter")
@Getter
@Setter
public class RollerShutterConfig {

    @Id
    private String name;

    @Nullable
    @Enumerated(EnumType.STRING)
    private CompassDirection compassDirection;

    @Nullable
    private LocalTime closeAt;

    @Nullable
    private LocalTime openAt;

}
