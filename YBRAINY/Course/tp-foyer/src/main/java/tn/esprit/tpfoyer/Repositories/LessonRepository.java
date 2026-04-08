package tn.esprit.tpfoyer.Repositories;

import tn.esprit.tpfoyer.Entities.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    Page<Lesson> findByCourseId(Long courseId, Pageable pageable);

    Page<Lesson> findAll(Pageable pageable);

    Optional<Lesson> findByIdAndCourseId(Long id, Long courseId);

    boolean existsByIdAndCourseId(Long id, Long courseId);

    @Query("SELECT MAX(l.orderIndex) FROM Lesson l WHERE l.course.id = :courseId")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") Long courseId);

    List<Lesson> findByTitleContainingIgnoreCase(String title);
}