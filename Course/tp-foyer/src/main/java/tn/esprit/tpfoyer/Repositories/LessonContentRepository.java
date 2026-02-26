package tn.esprit.tpfoyer.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.tpfoyer.Entities.LessonContent;

@Repository
public interface LessonContentRepository extends JpaRepository<LessonContent, Long> {
}
