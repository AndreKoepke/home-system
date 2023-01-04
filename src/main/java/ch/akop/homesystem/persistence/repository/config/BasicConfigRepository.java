package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.BasicConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BasicConfigRepository extends JpaRepository<BasicConfig, LocalDateTime> {


}
