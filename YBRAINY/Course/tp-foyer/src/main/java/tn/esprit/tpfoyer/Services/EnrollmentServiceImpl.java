package tn.esprit.tpfoyer.Services;

import tn.esprit.tpfoyer.Dto.CourseProgressDTO;
import tn.esprit.tpfoyer.Dto.EnrolledCourseDTO;
import tn.esprit.tpfoyer.Dto.EnrollmentDTO;
import tn.esprit.tpfoyer.Dto.LessonProgressDTO;
import tn.esprit.tpfoyer.Dto.StudentDashboardDTO;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.Enrollment;
import tn.esprit.tpfoyer.Entities.LessonProgress;
import tn.esprit.tpfoyer.Entities.enums.EnrollmentStatus;
import tn.esprit.tpfoyer.Entities.enums.ProgressStatus;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Repositories.EnrollmentRepository;
import tn.esprit.tpfoyer.Repositories.LessonProgressRepository;
import tn.esprit.tpfoyer.Repositories.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentServiceImpl implements IEnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    @Override
    public EnrollmentDTO enrollStudent(Long studentId, Long courseId) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new RuntimeException("Already enrolled");
        }
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .completionPercentage(0.0)
                .build();
        return toDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    public EnrollmentDTO enrollStudentWithPayment(Long studentId, Long courseId, String paymentIntentId) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new RuntimeException("Already enrolled");
        }
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .status(EnrollmentStatus.ACTIVE)
                .completionPercentage(0.0)
                .paymentIntentId(paymentIntentId)
                .build();
        return toDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressDTO getCourseProgress(Long courseId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found for studentId=" + studentId + " courseId=" + courseId));

        List<LessonProgressDTO> lessonProgresses = lessonProgressRepository
                .findByEnrollmentId(enrollment.getId()).stream()
                .map(this::toProgressDTO)
                .collect(Collectors.toList());

        return CourseProgressDTO.builder()
                .courseId(courseId)
                .enrollmentId(enrollment.getId())
                .completionPercentage(enrollment.getCompletionPercentage())
                .enrollmentStatus(enrollment.getStatus())
                .currentLessonId(enrollment.getCurrentLessonId())
                .lessonProgresses(lessonProgresses)
                .build();
    }

    @Override
    public CourseProgressDTO markLessonComplete(Long courseId, Long lessonId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found for studentId=" + studentId + " courseId=" + courseId));

        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .enrollmentId(enrollment.getId())
                        .lessonId(lessonId)
                        .build());

        // Always set COMPLETED regardless of current status (NOT_STARTED or IN_PROGRESS)
        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setCompletedAt(LocalDateTime.now());
        if (progress.getStartedAt() == null) {
            progress.setStartedAt(LocalDateTime.now());
        }
        progress = lessonProgressRepository.save(progress);
        System.out.println("[EnrollmentService] LessonProgress saved: id=" + progress.getId()
                + " lessonId=" + lessonId + " enrollmentId=" + enrollment.getId()
                + " status=" + progress.getStatus());

        enrollment.setCurrentLessonId(lessonId);

        int totalLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId).size();
        long completedCount = lessonProgressRepository.countByEnrollmentIdAndStatus(
                enrollment.getId(), ProgressStatus.COMPLETED);

        double percentage = totalLessons > 0 ? (completedCount / (double) totalLessons) * 100.0 : 0.0;
        enrollment.setCompletionPercentage(percentage);

        if (percentage >= 100.0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
            }
        }
        enrollmentRepository.save(enrollment);
        System.out.println("[EnrollmentService] Enrollment saved: id=" + enrollment.getId()
                + " completionPercentage=" + percentage
                + " completedCount=" + completedCount + "/" + totalLessons);

        List<LessonProgressDTO> lessonProgresses = lessonProgressRepository
                .findByEnrollmentId(enrollment.getId()).stream()
                .map(this::toProgressDTO)
                .collect(Collectors.toList());

        return CourseProgressDTO.builder()
                .courseId(courseId)
                .enrollmentId(enrollment.getId())
                .completionPercentage(enrollment.getCompletionPercentage())
                .enrollmentStatus(enrollment.getStatus())
                .currentLessonId(enrollment.getCurrentLessonId())
                .lessonProgresses(lessonProgresses)
                .build();
    }

    @Override
    public void trackTimeSpent(Long courseId, Long lessonId, Long studentId, Integer seconds) {
        enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).ifPresent(enrollment -> {
            LessonProgress progress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
                    .enrollmentId(enrollment.getId())
                    .lessonId(lessonId)
                    .status(ProgressStatus.NOT_STARTED)
                    .build());

            int current = progress.getTimeSpentSeconds() != null ? progress.getTimeSpentSeconds() : 0;
            progress.setTimeSpentSeconds(current + seconds);
            progress.setLastActivityAt(LocalDateTime.now());
            lessonProgressRepository.save(progress);

            System.out.println("TIME TRACKED: lessonId=" + lessonId
                + " studentId=" + studentId
                + " seconds=" + seconds
                + " total=" + progress.getTimeSpentSeconds());
        });
    }

    private EnrollmentDTO toDTO(Enrollment e) {
        return EnrollmentDTO.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .courseId(e.getCourseId())
                .enrollmentDate(e.getEnrollmentDate())
                .status(e.getStatus())
                .currentLessonId(e.getCurrentLessonId())
                .completionPercentage(e.getCompletionPercentage())
                .paymentIntentId(e.getPaymentIntentId())
                .completedAt(e.getCompletedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardDTO getStudentDashboard(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<EnrolledCourseDTO> enrolledCourses = new ArrayList<>();

        int totalCompleted = 0;
        int totalInProgress = 0;
        int totalCertificates = 0;
        double progressSum = 0.0;

        for (Enrollment enrollment : enrollments) {
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) continue;

            int totalLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(enrollment.getCourseId()).size();
            int completedLessons = (int) lessonProgressRepository
                    .countByEnrollmentIdAndStatus(enrollment.getId(), ProgressStatus.COMPLETED);

            Double bestQuizScore = getBestQuizScoreForCourse(studentId, enrollment.getCourseId());

            EnrolledCourseDTO dto = EnrolledCourseDTO.builder()
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .category(course.getCategory() != null ? course.getCategory().name() : null)
                    .level(course.getLevel() != null ? course.getLevel().name() : null)
                    .completionPercentage(enrollment.getCompletionPercentage() != null ? enrollment.getCompletionPercentage() : 0.0)
                    .enrollmentStatus(enrollment.getStatus().name())
                    .enrollmentDate(enrollment.getEnrollmentDate())
                    .completedAt(enrollment.getCompletedAt())
                    .certificateId(enrollment.getCertificateId())
                    .bestQuizScore(bestQuizScore)
                    .totalLessons(totalLessons)
                    .completedLessons(completedLessons)
                    .build();

            enrolledCourses.add(dto);

            double pct = enrollment.getCompletionPercentage() != null ? enrollment.getCompletionPercentage() : 0.0;
            progressSum += pct;
            if (EnrollmentStatus.COMPLETED.equals(enrollment.getStatus())) totalCompleted++;
            else totalInProgress++;
            if (enrollment.getCertificateId() != null) totalCertificates++;
        }

        double averageProgress = enrollments.isEmpty() ? 0.0 : progressSum / enrollments.size();

        return StudentDashboardDTO.builder()
                .studentId(studentId)
                .totalEnrolled(enrollments.size())
                .totalCompleted(totalCompleted)
                .totalInProgress(totalInProgress)
                .totalCertificates(totalCertificates)
                .averageProgress(averageProgress)
                .enrolledCourses(enrolledCourses)
                .build();
    }

    private Double getBestQuizScoreForCourse(Long studentId, Long courseId) {
        // Quiz data now lives in quiz-service; return null here
        return null;
    }

    private LessonProgressDTO toProgressDTO(LessonProgress p) {
        return LessonProgressDTO.builder()
                .id(p.getId())
                .enrollmentId(p.getEnrollmentId())
                .lessonId(p.getLessonId())
                .status(p.getStatus())
                .startedAt(p.getStartedAt())
                .completedAt(p.getCompletedAt())
                .timeSpentSeconds(p.getTimeSpentSeconds())
                .build();
    }
}
