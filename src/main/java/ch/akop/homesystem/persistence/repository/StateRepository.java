package ch.akop.homesystem.persistence.repository;

import ch.akop.homesystem.persistence.model.State;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface StateRepository extends JpaRepository<State, LocalDateTime> {

    @Nullable
    State getFirstByOrderByActivatedAtDesc();

}
