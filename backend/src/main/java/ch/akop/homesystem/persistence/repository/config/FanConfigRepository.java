package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.FanConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FanConfigRepository extends JpaRepository<FanConfig, String> {


}
