package tn.esprit.enrollmentservice.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tn.esprit.enrollmentservice.entities.*;
import tn.esprit.enrollmentservice.repositories.*;
import tn.esprit.enrollmentservice.clients.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock EnrollmentRepository enrollmentRepository;
    @Mock LessonClient lessonClient;
    @Mock CourseClient courseClient;
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks EnrollmentServiceImpl service;

    private Enrollment activeEnrollment() {
        Enrollment e = new Enrollment();
        e.setId(1L);
        e.setStudentId(10L);
        e.setCourseId(100L);
        e.setStatus(EnrollmentStatus.ACTIVE);
        e.setCompletionPercentage(0.0);
        return e;
    }

    @Test
    @DisplayName("Enrolling same student+course twice returns existing enrollment")
    void enroll_duplicate_returnsExisting() {
        Enrollment existing = activeEnrollment();
        when(courseClient.courseExists(100L)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 100L)).thenReturn(true);
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 100L))
            .thenReturn(Optional.of(existing));

        Enrollment result = service.enroll(10L, 100L);

        assertThat(result.getId()).isEqualTo(1L);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("New enrollment starts at 0% completion with ACTIVE status")
    void enroll_new_startsAtZeroPercent() {
        when(courseClient.courseExists(100L)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 100L)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Enrollment result = service.enroll(10L, 100L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(result.getCompletionPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Completing all lessons sets status to COMPLETED")
    void markLessonComplete_allLessons_setsCompleted() {
        Enrollment e = activeEnrollment();
        e.setCompletionPercentage(75.0);

        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 100L))
            .thenReturn(Optional.of(e));
        when(lessonClient.trackProgress(anyLong(), anyLong(), anyString(), any()))
            .thenReturn(Map.of());
        when(lessonClient.getCompletedLessonCount(1L)).thenReturn(4L);
        when(lessonClient.getLessons(100L)).thenReturn(List.of(
            Map.of("id", 1, "orderIndex", 0),
            Map.of("id", 2, "orderIndex", 1),
            Map.of("id", 3, "orderIndex", 2),
            Map.of("id", 4, "orderIndex", 3)
        ));
        when(lessonClient.getLessonCount(100L)).thenReturn(4L);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Enrollment result = service.markLessonComplete(100L, 4L, 10L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(result.getCompletionPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Completion percentage = completedLessons / totalLessons * 100")
    void markLessonComplete_partialCompletion_correctPercentage() {
        Enrollment e = activeEnrollment();

        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 100L))
            .thenReturn(Optional.of(e));
        when(lessonClient.trackProgress(anyLong(), anyLong(), anyString(), any()))
            .thenReturn(Map.of());
        when(lessonClient.getCompletedLessonCount(1L)).thenReturn(2L);
        when(lessonClient.getLessons(100L)).thenReturn(List.of(
            Map.of("id", 1, "orderIndex", 0),
            Map.of("id", 2, "orderIndex", 1),
            Map.of("id", 3, "orderIndex", 2),
            Map.of("id", 4, "orderIndex", 3)
        ));
        when(lessonClient.getLessonCount(100L)).thenReturn(4L);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Enrollment result = service.markLessonComplete(100L, 1L, 10L);

        assertThat(result.getCompletionPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getStudentEnrollments returns all enrollments for student")
    void getStudentEnrollments_returnsAll() {
        List<Enrollment> enrollments = List.of(activeEnrollment(), activeEnrollment());
        when(enrollmentRepository.findByStudentId(10L)).thenReturn(enrollments);

        List<Enrollment> result = service.getStudentEnrollments(10L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("isEnrolled returns true when enrollment exists")
    void isEnrolled_exists_returnsTrue() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 100L)).thenReturn(true);

        assertThat(service.isEnrolled(10L, 100L)).isTrue();
    }

    @Test
    @DisplayName("isEnrolled returns false when no enrollment")
    void isEnrolled_notExists_returnsFalse() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 100L)).thenReturn(false);

        assertThat(service.isEnrolled(10L, 100L)).isFalse();
    }

    @Test
    @DisplayName("updateCertificate persists certificateId to enrollment")
    void updateCertificate_persistsCertId() {
        Enrollment e = activeEnrollment();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCertificate(1L, "YBRY-2026-TEST-001");

        assertThat(e.getCertificateId()).isEqualTo("YBRY-2026-TEST-001");
        verify(enrollmentRepository).save(e);
    }
}
