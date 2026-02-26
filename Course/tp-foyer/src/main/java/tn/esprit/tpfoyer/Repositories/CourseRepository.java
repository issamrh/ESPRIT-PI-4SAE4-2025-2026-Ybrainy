package tn.esprit.tpfoyer.Repositories;

import tn.esprit.tpfoyer.Entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    boolean existsByTitle(String title);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.course.id = :courseId")
    Integer countLessonsByCourseId(@Param("courseId") Long courseId);
}
