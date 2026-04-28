package tn.esprit.tpfoyer.Services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.enums.CourseCategory;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Repositories.CourseReviewRepository;
import tn.esprit.tpfoyer.Clients.EnrollmentClient;
import tn.esprit.tpfoyer.Clients.LessonClient;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock LessonClient lessonClient;
    @Mock EnrollmentClient enrollmentClient;
    @Mock IFileStorageService fileStorageService;
    @Mock IAiSearchService aiSearchService;
    @Mock CourseReviewRepository courseReviewRepository;
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks CourseServiceImpl service;

    private Course sampleCourse() {
        Course c = new Course();
        c.setId(1L);
        c.setTitle("Test Course");
        c.setIsPublished(false);
        c.setInstructorId(5L);
        return c;
    }

    @Test
    @DisplayName("getCourseStatsByCategory groups courses by category")
    void getCourseStatsByCategory_groupsByCategory() {
        Course c = sampleCourse();
        c.setCategory(CourseCategory.PROGRAMMING);
        when(courseRepository.findAll()).thenReturn(List.of(c));

        Map<String, Long> result = service.getCourseStatsByCategory();

        assertThat(result).containsKey("PROGRAMMING");
        assertThat(result.get("PROGRAMMING")).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCourseById throws when course not found")
    void getCourseById_notFound_throws() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCourseById(999L))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("togglePublish sets isPublished to true")
    void togglePublish_setsPublished() {
        Course c = sampleCourse();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        Map<String, Object> result = service.togglePublish(1L, true, 5L, "INSTRUCTOR");

        assertThat(result.get("isPublished")).isEqualTo(true);
    }

    @Test
    @DisplayName("togglePublish by non-owner throws access denied")
    void togglePublish_nonOwner_throws() {
        Course c = sampleCourse();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.togglePublish(1L, true, 99L, "INSTRUCTOR"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("own courses");
    }

    @Test
    @DisplayName("Admin can publish any course regardless of ownership")
    void togglePublish_adminCanPublishAny() {
        Course c = sampleCourse();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        Map<String, Object> result = service.togglePublish(1L, true, 99L, "ADMIN");

        assertThat(result.get("isPublished")).isEqualTo(true);
    }

    @Test
    @DisplayName("deleteCourse by non-owner instructor throws access denied")
    void deleteCourse_nonOwner_throws() {
        Course c = sampleCourse(); // instructorId = 5L
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.deleteCourse(1L, 99L, "INSTRUCTOR"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("getCourseStatsByCategory returns empty map when no courses have categories")
    void getCourseStatsByCategory_noCourses_returnsEmpty() {
        when(courseRepository.findAll()).thenReturn(List.of());

        Map<String, Long> result = service.getCourseStatsByCategory();

        assertThat(result).isEmpty();
    }
}
