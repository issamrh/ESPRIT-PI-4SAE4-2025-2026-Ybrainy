package tn.esprit.ybrainy_mevents.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.ybrainy_mevents.Entities.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
}
