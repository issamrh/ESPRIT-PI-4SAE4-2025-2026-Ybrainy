package tn.esprit.lessonservice.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.lessonservice.clients.CourseClient;
import tn.esprit.lessonservice.entities.Lesson;
import tn.esprit.lessonservice.entities.LessonProgress;
import tn.esprit.lessonservice.entities.LessonType;
import tn.esprit.lessonservice.entities.ProgressStatus;
import tn.esprit.lessonservice.repositories.LessonProgressRepository;
import tn.esprit.lessonservice.repositories.LessonRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock LessonProgressRepository lessonProgressRepository;
    @Mock CourseClient courseClient;
    @InjectMocks LessonServiceImpl service;

    private Lesson sampleLesson(Long id, Long courseId) {
        Lesson l = new Lesson();
        l.setId(id);
        l.setCourseId(courseId);
        l.setTitle("Intro to Java");
        l.setType(LessonType.VIDEO_UPLOAD);
        l.setOrderIndex(1);
        l.setDurationMinutes(30);
        return l;
    }

    @Test
    @DisplayName("getLessonsByCourse returns all lessons for the given course")
    void getLessonsByCourse_returnsList() {
        when(lessonRepository.findByCourseIdWithContents(10L))
            .thenReturn(List.of(sampleLesson(1L, 10L), sampleLesson(2L, 10L)));

        List<Lesson> result = service.getLessonsByCourse(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCourseId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getLessonById throws when lesson is not found")
    void getLessonById_notFound_throws() {
        when(lessonRepository.findByIdWithContents(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLessonById(99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("createLesson verifies course exists before saving")
    void createLesson_validCourse_saves() {
        when(courseClient.courseExists(10L)).thenReturn(true);
        when(lessonRepository.findMaxOrderIndexByCourseId(10L)).thenReturn(Optional.of(2));
        Lesson lesson = sampleLesson(null, null);
        lesson.setOrderIndex(null);
        when(lessonRepository.save(any())).thenAnswer(i -> {
            Lesson l = i.getArgument(0);
            l.setId(1L);
            return l;
        });

        Lesson result = service.createLesson(10L, lesson);

        assertThat(result.getCourseId()).isEqualTo(10L);
        assertThat(result.getOrderIndex()).isEqualTo(3);
        verify(lessonRepository).save(lesson);
    }

    @Test
    @DisplayName("createLesson throws when course does not exist")
    void createLesson_courseNotFound_throws() {
        when(courseClient.courseExists(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.createLesson(999L, sampleLesson(null, null)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");

        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteLesson removes the lesson when found in the course")
    void deleteLesson_existing_removes() {
        Lesson lesson = sampleLesson(1L, 10L);
        when(lessonRepository.findByIdAndCourseId(1L, 10L)).thenReturn(Optional.of(lesson));

        service.deleteLesson(10L, 1L);

        verify(lessonRepository).delete(lesson);
    }

    @Test
    @DisplayName("countLessonsByCourse delegates to repository")
    void countLessonsByCourse_delegatesToRepo() {
        when(lessonRepository.countByCourseId(10L)).thenReturn(5L);

        assertThat(service.countLessonsByCourse(10L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("trackProgress creates new progress when none exists")
    void trackProgress_newProgress_saved() {
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(1L, 1L))
            .thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LessonProgress result = service.trackProgress(1L, 1L, "IN_PROGRESS", null);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        verify(lessonProgressRepository).save(any(LessonProgress.class));
    }

    @Test
    @DisplayName("trackProgress sets completedAt when status transitions to COMPLETED")
    void trackProgress_completed_setsCompletedAt() {
        LessonProgress existing = LessonProgress.builder()
            .enrollmentId(1L)
            .lessonId(1L)
            .status(ProgressStatus.IN_PROGRESS)
            .build();
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(1L, 1L))
            .thenReturn(Optional.of(existing));
        when(lessonProgressRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LessonProgress result = service.trackProgress(1L, 1L, "COMPLETED", null);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("trackProgress accumulates timeSpent on repeated calls")
    void trackProgress_accumulatesTime() {
        LessonProgress existing = LessonProgress.builder()
            .enrollmentId(1L)
            .lessonId(1L)
            .status(ProgressStatus.IN_PROGRESS)
            .timeSpentSeconds(120L)
            .build();
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(1L, 1L))
            .thenReturn(Optional.of(existing));
        when(lessonProgressRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LessonProgress result = service.trackProgress(1L, 1L, "IN_PROGRESS", 60L);

        assertThat(result.getTimeSpentSeconds()).isEqualTo(180L);
    }

    @Test
    @DisplayName("countCompletedLessons delegates to repository with COMPLETED status")
    void countCompletedLessons_delegatesToRepo() {
        when(lessonProgressRepository.countByEnrollmentIdAndStatus(1L, ProgressStatus.COMPLETED))
            .thenReturn(4L);

        assertThat(service.countCompletedLessons(1L)).isEqualTo(4L);
    }

    @Test
    @DisplayName("getProgressByEnrollment returns progress records for enrollment")
    void getProgressByEnrollment_returnsList() {
        LessonProgress p = new LessonProgress();
        p.setEnrollmentId(1L);
        when(lessonProgressRepository.findByEnrollmentId(1L)).thenReturn(List.of(p));

        List<LessonProgress> result = service.getProgressByEnrollment(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEnrollmentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("searchLessonsByTitle returns matching lessons case-insensitively")
    void searchLessonsByTitle_returnsMatches() {
        when(lessonRepository.findByTitleContainingIgnoreCase("java"))
            .thenReturn(List.of(sampleLesson(1L, 10L)));

        List<Lesson> result = service.searchLessonsByTitle("java");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).containsIgnoringCase("Java");
    }
}
