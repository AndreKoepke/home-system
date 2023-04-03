package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.DeconzConfig;
import java.time.LocalDateTime;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeconzConfigRepository extends JpaRepository<DeconzConfig, LocalDateTime> {

    @Nullable
    DeconzConfig getFirstByOrderByModifiedDesc();

}
