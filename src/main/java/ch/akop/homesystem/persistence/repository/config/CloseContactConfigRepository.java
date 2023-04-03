package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.CloseContactConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloseContactConfigRepository extends JpaRepository<CloseContactConfig, String> {


}
