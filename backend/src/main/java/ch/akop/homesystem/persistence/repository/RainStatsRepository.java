package ch.akop.homesystem.persistence.repository;

import ch.akop.homesystem.persistence.model.RainStats;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RainStatsRepository extends JpaRepository<RainStats, LocalDateTime> {

  RainStats findFirstByOrderByMeasuredAtDesc();

}
