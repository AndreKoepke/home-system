package ch.akop.homesystem.authentication;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface AuthenticationRepository extends JpaRepository<AuthenticationToken, UUID> {

  Optional<AuthenticationToken> findByToken(String token);
}
