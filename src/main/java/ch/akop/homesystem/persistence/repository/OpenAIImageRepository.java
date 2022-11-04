package ch.akop.homesystem.persistence.repository;

import ch.akop.homesystem.persistence.model.ImageOfOpenAI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OpenAIImageRepository extends JpaRepository<ImageOfOpenAI, LocalDateTime> {
    Optional<ImageOfOpenAI> findFirstByOrderByCreatedDesc();
}
