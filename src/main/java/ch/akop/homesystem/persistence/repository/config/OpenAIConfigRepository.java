package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.OpenAIConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OpenAIConfigRepository extends JpaRepository<OpenAIConfig, LocalDateTime> {

    Optional<OpenAIConfig> findFirstByOrderByModifiedDesc();

}
