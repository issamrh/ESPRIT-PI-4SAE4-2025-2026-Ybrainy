package tn.esprit.tpfoyer.Services;

import tn.esprit.tpfoyer.Dto.CourseDetailResponseDTO;
import tn.esprit.tpfoyer.Dto.CourseRequestDTO;
import tn.esprit.tpfoyer.Dto.CourseResponseDTO;
import tn.esprit.tpfoyer.Dto.LessonContentResponseDTO;
import tn.esprit.tpfoyer.Dto.LessonResponseDTO;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.Lesson;
import tn.esprit.tpfoyer.Entities.LessonContent;
import tn.esprit.tpfoyer.Entities.enums.CourseCategory;
import tn.esprit.tpfoyer.Entities.enums.CourseLevel;
import tn.esprit.tpfoyer.Entities.enums.LessonType;
import tn.esprit.tpfoyer.Exception.ResourceNotFoundException;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.tpfoyer.Repositories.CourseSpecification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final IFileStorageService fileStorageService;

    @Override
    public CourseResponseDTO createCourse(CourseRequestDTO dto, MultipartFile thumbnail, MultipartFile certificate) {
        Course course = Course.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .category(dto.getCategory())
                .level(dto.getLevel())
                .approximateDurationMinutes(dto.getApproximateDurationMinutes())
                .isPublished(dto.getIsPublished() != null ? dto.getIsPublished() : false)
                .offersCertificate(dto.getOffersCertificate() != null ? dto.getOffersCertificate() : false)
                .build();

        if (thumbnail != null && !thumbnail.isEmpty()) {
            course.setThumbnailUrl(fileStorageService.storeThumbnail(thumbnail));
        }

        if (Boolean.TRUE.equals(course.getOffersCertificate())) {
            if (certificate == null || certificate.isEmpty()) {
                throw new IllegalArgumentException("Certificate file is required when offersCertificate is true");
            }
            course.setCertificatePath(fileStorageService.storeCertificate(certificate));
        }

        return toResponseDTO(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponseDTO> getAllCourses(
            String search,
            CourseCategory category,
            CourseLevel level,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isPublished,
            Pageable pageable) {

        Specification<Course> spec = Specification
                .where(CourseSpecification.searchByKeyword(search))
                .and(CourseSpecification.hasCategory(category))
                .and(CourseSpecification.hasLevel(level))
                .and(CourseSpecification.hasPriceGreaterThanOrEqual(minPrice))
                .and(CourseSpecification.hasPriceLessThanOrEqual(maxPrice))
                .and(CourseSpecification.isPublished(isPublished));

        return courseRepository.findAll(spec, pageable).map(this::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponseDTO getCourseById(Long id) {
        return mapToDetailDTO(findOrThrow(id));
    }

    @Override
    public CourseResponseDTO updateCourse(Long id, CourseRequestDTO dto, MultipartFile thumbnail, MultipartFile certificate) {
        Course course = findOrThrow(id);

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setPrice(dto.getPrice());
        course.setCategory(dto.getCategory());
        course.setLevel(dto.getLevel());
        course.setApproximateDurationMinutes(dto.getApproximateDurationMinutes());
        if (dto.getIsPublished() != null) {
            course.setIsPublished(dto.getIsPublished());
        }

        if (dto.getOffersCertificate() != null) {
            course.setOffersCertificate(dto.getOffersCertificate());
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            fileStorageService.deleteFile(course.getThumbnailUrl());
            course.setThumbnailUrl(fileStorageService.storeThumbnail(thumbnail));
        }

        if (Boolean.TRUE.equals(course.getOffersCertificate())) {
            if (certificate != null && !certificate.isEmpty()) {
                fileStorageService.deleteFile(course.getCertificatePath());
                course.setCertificatePath(fileStorageService.storeCertificate(certificate));
            } else if (course.getCertificatePath() == null || course.getCertificatePath().isBlank()) {
                throw new IllegalArgumentException("Certificate file is required when offersCertificate is true");
            }
        } else {
            fileStorageService.deleteFile(course.getCertificatePath());
            course.setCertificatePath(null);
        }

        return toResponseDTO(courseRepository.save(course));
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = findOrThrow(id);
        fileStorageService.deleteFile(course.getThumbnailUrl());
        fileStorageService.deleteFile(course.getCertificatePath());
        Optional.ofNullable(course.getLessons()).orElse(Collections.emptyList())
                .forEach(lesson -> {
                    Optional.ofNullable(lesson.getContents()).orElse(Collections.emptyList())
                            .forEach(c -> {
                                if (c.getType() != LessonType.YOUTUBE_EMBED) {
                                    fileStorageService.deleteFile(c.getContentUrl());
                                }
                            });
                    // legacy
                    fileStorageService.deleteFile(lesson.getContentUrl());
                });
        courseRepository.delete(course);
    }

    private Course findOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private CourseResponseDTO toResponseDTO(Course course) {
        Integer lessonCount = courseRepository.countLessonsByCourseId(course.getId());
        return CourseResponseDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .rating(course.getRating())
                .ratingCount(course.getRatingCount())
                .category(course.getCategory())
                .level(course.getLevel())
                .approximateDurationMinutes(course.getApproximateDurationMinutes())
                .isPublished(course.getIsPublished())
                .offersCertificate(course.getOffersCertificate())
                .lessonCount(lessonCount != null ? lessonCount : 0)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private LessonResponseDTO toLessonResponseDTO(Lesson lesson) {
        return LessonResponseDTO.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .type(lesson.getType())
                .contentUrl(lesson.getContentUrl())
                .contents(Optional.ofNullable(lesson.getContents()).orElse(Collections.emptyList())
                        .stream()
                        .map(this::toLessonContentResponseDTO)
                        .collect(Collectors.toList()))
                .orderIndex(lesson.getOrderIndex())
                .durationMinutes(lesson.getDurationMinutes())
                .courseId(lesson.getCourse() != null ? lesson.getCourse().getId() : null)
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    private LessonContentResponseDTO toLessonContentResponseDTO(LessonContent content) {
        return LessonContentResponseDTO.builder()
                .id(content.getId())
                .type(content.getType())
                .contentUrl(content.getContentUrl())
                .createdAt(content.getCreatedAt())
                .build();
    }

    private CourseDetailResponseDTO mapToDetailDTO(Course course) {
        CourseResponseDTO base = toResponseDTO(course);

        List<LessonResponseDTO> lessons = Optional.ofNullable(course.getLessons())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toLessonResponseDTO)
                .collect(Collectors.toList());
        int lessonCount = lessons.size();
        return CourseDetailResponseDTO.builder()
                .id(base.getId())
                .title(base.getTitle())
                .description(base.getDescription())
                .thumbnailUrl(base.getThumbnailUrl())
                .price(base.getPrice())
                .rating(base.getRating())
                .ratingCount(base.getRatingCount())
                .category(base.getCategory())
                .level(base.getLevel())
                .approximateDurationMinutes(base.getApproximateDurationMinutes())
                .isPublished(base.getIsPublished())
                .offersCertificate(base.getOffersCertificate())
                .lessonCount(lessonCount)
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .lessons(lessons)
                .build();
    }
}