package ch.akop.homesystem.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
