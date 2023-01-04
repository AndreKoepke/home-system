package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.MastodonConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MastodonConfigRepository extends JpaRepository<MastodonConfig, LocalDateTime> {

    Optional<MastodonConfig> findFirstByOrderByModifiedDesc();

}
