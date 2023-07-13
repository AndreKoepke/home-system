package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.BasicConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BasicConfigRepository extends JpaRepository<BasicConfig, LocalDateTime> {

  Optional<BasicConfig> findByOrderByModifiedDesc();

}
