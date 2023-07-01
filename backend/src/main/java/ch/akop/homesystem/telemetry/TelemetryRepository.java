package ch.akop.homesystem.telemetry;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryRepository extends JpaRepository<TelemetryData, UUID> {

}
