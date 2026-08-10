package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.MotionSensorConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotionSensorConfigRepository extends JpaRepository<MotionSensorConfig, String> {

  Optional<MotionSensorConfig> findByName(String name);

}
