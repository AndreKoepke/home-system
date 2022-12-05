package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.TelegramConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TelegramConfigRepository extends JpaRepository<TelegramConfig, LocalDateTime> {


}
