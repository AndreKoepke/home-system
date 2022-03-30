package ch.akop.homesystem.persistence.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "rain_stats")
@Getter
@Setter
public class RainStats {
    @Id
    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt = LocalDateTime.now();

    private boolean raining;
}
