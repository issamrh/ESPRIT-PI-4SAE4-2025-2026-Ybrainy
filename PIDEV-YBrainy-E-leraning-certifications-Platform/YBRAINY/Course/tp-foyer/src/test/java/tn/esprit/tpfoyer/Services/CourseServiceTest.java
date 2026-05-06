package tn.esprit.tpfoyer.Services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tn.esprit.tpfoyer.Dto.CourseRequestDTO;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.enums.CourseCategory;
import tn.esprit.tpfoyer.Entities.enums.CourseLevel;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Repositories.CourseReviewRepository;
import tn.esprit.tpfoyer.Clients.EnrollmentClient;
import tn.esprit.tpfoyer.Clients.LessonClient;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

        assertThat(result).containsEntry("PROGRAMMING", 1L);
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

        assertThat(result).containsEntry("isPublished", true);
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

        assertThat(result).containsEntry("isPublished", true);
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

    @Test
    @DisplayName("existsById returns true when course exists")
    void existsById_returnsTrue() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        assertThat(service.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("existsById returns false when course does not exist")
    void existsById_returnsFalse() {
        when(courseRepository.existsById(999L)).thenReturn(false);
        assertThat(service.existsById(999L)).isFalse();
    }

    @Test
    @DisplayName("createCourse without thumbnail or certificate saves and returns DTO")
    void createCourse_basic_savesAndReturnsDTO() {
        CourseRequestDTO dto = new CourseRequestDTO();
        dto.setTitle("Java Basics");
        dto.setDescription("Learn Java");
        dto.setPrice(BigDecimal.valueOf(29.99));
        dto.setCategory(CourseCategory.PROGRAMMING);
        dto.setLevel(CourseLevel.BEGINNER);
        dto.setInstructorId(10L);
        dto.setIsPublished(false);
        dto.setOffersCertificate(false);

        Course saved = new Course();
        saved.setId(42L);
        saved.setTitle("Java Basics");
        saved.setCategory(CourseCategory.PROGRAMMING);
        when(courseRepository.save(any(Course.class))).thenReturn(saved);

        var result = service.createCourse(dto, null, null);

        assertThat(result).isNotNull();
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("createCourse with offersCertificate=true but no file throws")
    void createCourse_certificateRequired_throws() {
        CourseRequestDTO dto = new CourseRequestDTO();
        dto.setTitle("Advanced Java");
        dto.setInstructorId(10L);
        dto.setOffersCertificate(true);

        assertThatThrownBy(() -> service.createCourse(dto, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Certificate file is required");
    }

    @Test
    @DisplayName("getCourseStatsByCategory counts multiple categories")
    void getCourseStatsByCategory_multipleCourses_correctCounts() {
        Course c1 = new Course(); c1.setCategory(CourseCategory.PROGRAMMING);
        Course c2 = new Course(); c2.setCategory(CourseCategory.PROGRAMMING);
        Course c3 = new Course(); c3.setCategory(CourseCategory.DESIGN);
        Course c4 = new Course(); // null category — should be excluded
        when(courseRepository.findAll()).thenReturn(List.of(c1, c2, c3, c4));

        Map<String, Long> result = service.getCourseStatsByCategory();

        assertThat(result).containsEntry("PROGRAMMING", 2L).containsEntry("DESIGN", 1L).doesNotContainKey(null);
    }

    @Test
    @DisplayName("deleteCourse by admin succeeds regardless of ownership")
    void deleteCourse_admin_succeeds() {
        Course c = sampleCourse();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatNoException().isThrownBy(() -> service.deleteCourse(1L, 999L, "ADMIN"));
        verify(courseRepository).delete(c);
    }

    @Test
    @DisplayName("deleteCourse by owner instructor succeeds")
    void deleteCourse_ownerInstructor_succeeds() {
        Course c = sampleCourse(); // instructorId = 5L
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatNoException().isThrownBy(() -> service.deleteCourse(1L, 5L, "INSTRUCTOR"));
        verify(courseRepository).delete(c);
    }

    @Test
    @DisplayName("togglePublish sets isPublished to false")
    void togglePublish_setsUnpublished() {
        Course c = sampleCourse();
        c.setIsPublished(true);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        Map<String, Object> result = service.togglePublish(1L, false, 5L, "INSTRUCTOR");

        assertThat(result).containsEntry("isPublished", false);
    }
}
