package ch.akop.homesystem.persistence.repository;

import ch.akop.homesystem.persistence.model.RainStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RainStatsRepository extends JpaRepository<RainStats, LocalDateTime> {

    Optional<RainStats> findFirstByOrderByMeasuredAtDesc();

}
