package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.config.OffButtonConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

@Repository
public interface OffButtonConfigRepository extends JpaRepository<OffButtonConfig, String> {

    Stream<OffButtonConfig> findAllByNameAndButtonEvent(String name, Integer buttonEvent);

}
