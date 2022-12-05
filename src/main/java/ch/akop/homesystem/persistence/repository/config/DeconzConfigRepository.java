package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.model.config.DeconzConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DeconzConfigRepository extends JpaRepository<DeconzConfig, LocalDateTime> {
    DeconzConfig findFirstByOrderByModifiedDesc();

}
