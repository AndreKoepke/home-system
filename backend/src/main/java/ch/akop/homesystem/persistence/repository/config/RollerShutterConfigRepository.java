package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RollerShutterConfigRepository extends JpaRepository<RollerShutterConfig, String> {

  Stream<RollerShutterConfig> findByCompassDirection(CompassDirection compassDirection);

  Stream<RollerShutterConfig> findRollerShutterConfigByCompassDirectionIsNotNull();

}
