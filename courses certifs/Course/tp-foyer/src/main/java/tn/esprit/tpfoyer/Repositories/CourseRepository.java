package tn.esprit.tpfoyer.Repositories;

import tn.esprit.tpfoyer.Entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    boolean existsByTitle(String title);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.course.id = :courseId")
    Integer countLessonsByCourseId(@Param("courseId") Long courseId);

    /**
     * Batch query to get lesson counts for multiple courses at once.
     * Returns a projection with courseId and count.
     * Use this to avoid N+1 queries when mapping course lists.
     */
    @Query("SELECT l.course.id as courseId, COUNT(l) as lessonCount FROM Lesson l " +
           "WHERE l.course.id IN :courseIds " +
           "GROUP BY l.course.id")
    List<Object[]> countLessonsByCourseIds(@Param("courseIds") List<Long> courseIds);
}
