package tn.esprit.tpfoyer.Repositories;

import tn.esprit.tpfoyer.Entities.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Enrollment> findByCourseId(Long courseId);

    void deleteAllByCourseId(Long courseId);

    long countByCourseIdAndEnrollmentDateAfter(Long courseId, LocalDateTime date);

    Optional<Enrollment> findByCertificateId(String certificateId);
}
