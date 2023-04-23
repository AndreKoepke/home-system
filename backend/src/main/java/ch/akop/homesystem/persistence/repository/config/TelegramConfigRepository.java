package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.TelegramConfig;
import java.time.LocalDateTime;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramConfigRepository extends JpaRepository<TelegramConfig, LocalDateTime> {

  @Nullable
  TelegramConfig getFirstByOrderByModifiedDesc();
}
