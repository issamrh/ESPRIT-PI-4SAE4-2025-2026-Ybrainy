package tn.esprit.tpfoyer.Services;

import tn.esprit.tpfoyer.Dto.CourseProgressDTO;
import tn.esprit.tpfoyer.Dto.EnrollmentDTO;
import tn.esprit.tpfoyer.Dto.StudentDashboardDTO;

import java.util.List;

public interface IEnrollmentService {

    EnrollmentDTO enrollStudent(Long studentId, Long courseId);

    EnrollmentDTO enrollStudentWithPayment(Long studentId, Long courseId, String paymentIntentId);

    List<EnrollmentDTO> getEnrollmentsByStudent(Long studentId);

    boolean isEnrolled(Long studentId, Long courseId);

    CourseProgressDTO getCourseProgress(Long courseId, Long studentId);

    CourseProgressDTO markLessonComplete(Long courseId, Long lessonId, Long studentId);

    void trackTimeSpent(Long courseId, Long lessonId, Long studentId, Integer seconds);

    StudentDashboardDTO getStudentDashboard(Long studentId);
}
