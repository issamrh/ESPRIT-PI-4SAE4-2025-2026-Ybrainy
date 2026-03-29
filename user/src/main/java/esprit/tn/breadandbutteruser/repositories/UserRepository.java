package esprit.tn.breadandbutteruser.repositories;

import esprit.tn.breadandbutteruser.entities.User;
import esprit.tn.breadandbutteruser.entities.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKeycloakUserId(String keycloakUserId);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByFaceBiometricHash(String faceBiometricHash);
    boolean existsByKeycloakUserId(String keycloakUserId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByRole(Role role);
    Optional<User> findByPersonalityPersonalityId(Long personalityId);
    Optional<User> findByPersonalityBehaviorBehaviorId(Long behaviorId);
}
