package com.esprit.demo.Repositories;

import com.esprit.demo.Models.UserXpEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserXpEventRepository extends JpaRepository<UserXpEvent, Long> {

    /** Historique complet d'un utilisateur (du plus récent au plus ancien) */
    List<UserXpEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Les 10 derniers événements XP */
    List<UserXpEvent> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /** Les 30 derniers événements XP en ordre chronologique ASC (pour le graphe) */
    List<UserXpEvent> findTop30ByUserIdOrderByCreatedAtAsc(Long userId);
}
