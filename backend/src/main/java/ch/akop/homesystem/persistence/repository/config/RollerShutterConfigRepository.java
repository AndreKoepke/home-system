package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.RollerShutterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface RollerShutterConfigRepository extends JpaRepository<RollerShutterConfig, String> {

  Optional<RollerShutterConfig> findByNameLike(String name);

  Stream<RollerShutterConfig> findRollerShutterConfigByCompassDirectionIsNotNull();

}
