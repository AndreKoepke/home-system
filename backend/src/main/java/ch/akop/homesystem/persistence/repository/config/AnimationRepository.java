package ch.akop.homesystem.persistence.repository.config;

import ch.akop.homesystem.persistence.model.animation.Animation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimationRepository extends JpaRepository<Animation, UUID> {

}
