package ch.akop.homesystem.persistence.repository;

import ch.akop.homesystem.persistence.model.ImageOfOpenAI;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OpenAIImageRepository extends JpaRepository<ImageOfOpenAI, LocalDateTime> {

  Optional<ImageOfOpenAI> findFirstByOrderByCreatedDesc();

  @Modifying
  @Query("update ImageOfOpenAI i set i.downloaded = i.downloaded + 1 where i.created = ?1")
  void increaseDownloadCounter(LocalDateTime createdAt);
}
