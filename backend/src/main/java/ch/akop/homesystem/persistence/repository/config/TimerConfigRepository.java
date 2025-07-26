package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.TimerConfig;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerConfigRepository extends JpaRepository<TimerConfig, UUID> {


}
