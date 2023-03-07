package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.DeconzConfig;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DeconzConfigRepository extends JpaRepository<DeconzConfig, LocalDateTime> {

    @Nullable
    DeconzConfig getFirstByOrderByModifiedDesc();

}
