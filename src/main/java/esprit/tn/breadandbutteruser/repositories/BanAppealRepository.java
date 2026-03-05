package esprit.tn.breadandbutteruser.repositories;

import esprit.tn.breadandbutteruser.entities.BanAppeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BanAppealRepository extends JpaRepository<BanAppeal, Long> {
    List<BanAppeal> findByUserUserId(Long userId);
    List<BanAppeal> findByAppealStatus(String appealStatus);
}
