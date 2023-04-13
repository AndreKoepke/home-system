package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.MastodonConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MastodonConfigRepository extends JpaRepository<MastodonConfig, LocalDateTime> {

  Optional<MastodonConfig> findFirstByOrderByModifiedDesc();

}
