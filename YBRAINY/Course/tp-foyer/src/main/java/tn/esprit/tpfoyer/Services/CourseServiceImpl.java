package tn.esprit.tpfoyer.Services;

import tn.esprit.tpfoyer.Dto.AiSearchIntentDTO;
import tn.esprit.tpfoyer.Dto.AiSearchResultDTO;
import tn.esprit.tpfoyer.Dto.CourseAnalyticsDTO;
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
import tn.esprit.tpfoyer.Entities.Enrollment;
import tn.esprit.tpfoyer.Entities.enums.EnrollmentStatus;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Repositories.CourseReviewRepository;
import tn.esprit.tpfoyer.Repositories.EnrollmentRepository;
import tn.esprit.tpfoyer.Repositories.LessonProgressRepository;
import tn.esprit.tpfoyer.Repositories.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.tpfoyer.Repositories.CourseSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final IFileStorageService fileStorageService;
    private final IAiSearchService aiSearchService;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final RestTemplate restTemplate;

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
                .instructorId(dto.getInstructorId())
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
            Long instructorId,
            Pageable pageable) {

        Specification<Course> spec = Specification
                .where(CourseSpecification.searchByKeyword(search))
                .and(CourseSpecification.hasCategory(category))
                .and(CourseSpecification.hasLevel(level))
                .and(CourseSpecification.hasPriceGreaterThanOrEqual(minPrice))
                .and(CourseSpecification.hasPriceLessThanOrEqual(maxPrice))
                .and(CourseSpecification.isPublished(isPublished))
                .and(CourseSpecification.hasInstructorId(instructorId));

        Page<Course> coursePage = courseRepository.findAll(spec, pageable);
        
        // Batch fetch lesson counts to avoid N+1 query problem
        List<Long> courseIds = coursePage.getContent().stream()
                .map(Course::getId)
                .collect(Collectors.toList());
        
        Map<Long, Integer> lessonCountMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<Object[]> counts = courseRepository.countLessonsByCourseIds(courseIds);
            for (Object[] row : counts) {
                Long courseId = (Long) row[0];
                Long count = (Long) row[1];
                lessonCountMap.put(courseId, count.intValue());
            }
        }
        
        return coursePage.map(course -> toResponseDTO(course, lessonCountMap));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponseDTO getCourseById(Long id) {
        return mapToDetailDTO(findOrThrow(id));
    }

    @Override
    public CourseResponseDTO updateCourse(Long id, CourseRequestDTO dto, MultipartFile thumbnail, MultipartFile certificate, Long requestingUserId, String requestingRole) {
        Course course = findOrThrow(id);
        validateOwnership(course, requestingUserId, requestingRole);

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setPrice(dto.getPrice());
        course.setCategory(dto.getCategory());
        course.setLevel(dto.getLevel());
        course.setApproximateDurationMinutes(dto.getApproximateDurationMinutes());
        course.setInstructorId(dto.getInstructorId());
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
    public void deleteCourse(Long id, Long requestingUserId, String requestingRole) {
        Course course = findOrThrow(id);
        validateOwnership(course, requestingUserId, requestingRole);

        // Delete LessonProgress records for all Enrollments of this course
        List<Long> enrollmentIds = enrollmentRepository.findByCourseId(id)
                .stream().map(Enrollment::getId).collect(Collectors.toList());
        if (!enrollmentIds.isEmpty()) {
            lessonProgressRepository.deleteAllByEnrollmentIdIn(enrollmentIds);
        }
        // Delete all Enrollments for this course
        enrollmentRepository.deleteAllByCourseId(id);

        // Delete all CourseReviews for this course
        courseReviewRepository.deleteAllByCourseId(id);

        // Delete course files and the course itself
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
        // Delete quizzes for this course from Quiz Service
        try {
            restTemplate.delete("http://localhost:8083/api/quizzes/course/" + id);
        } catch (Exception e) {
            log.warn("Could not delete quizzes for course {}: {}", id, e.getMessage());
        }

        courseRepository.delete(course);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseAnalyticsDTO getCourseAnalytics(Long courseId) {
        Course course = findOrThrow(courseId);
        
        // Get lesson statistics
        List<Lesson> lessons = Optional.ofNullable(course.getLessons()).orElse(Collections.emptyList());
        int totalLessons = lessons.size();
        
        // Calculate total video minutes and material counts
        long totalVideoMinutes = 0;
        int totalPdfMaterials = 0;
        int totalImageMaterials = 0;
        
        for (Lesson lesson : lessons) {
            if (lesson.getDurationMinutes() != null) {
                totalVideoMinutes += lesson.getDurationMinutes();
            }
            
            List<LessonContent> contents = Optional.ofNullable(lesson.getContents()).orElse(Collections.emptyList());
            for (LessonContent content : contents) {
                switch (content.getType()) {
                    case PDF -> totalPdfMaterials++;
                    case IMAGE -> totalImageMaterials++;
                    default -> {}
                }
            }
        }
        
        // Real enrollment metrics (guarded against missing data / repository errors)
        int totalEnrollments = 0;
        int completedStudents = 0;
        double averageProgress = 0.0;
        int activeStudents7 = 0;
        int activeStudents30 = 0;
        double totalRevenue = 0.0;
        double monthlyRevenue = 0.0;
        List<Enrollment> enrollments = Collections.emptyList();

        try {
            enrollments = enrollmentRepository.findByCourseId(courseId);
            totalEnrollments = enrollments.size();
            completedStudents = (int) enrollments.stream()
                    .filter(e -> EnrollmentStatus.COMPLETED.equals(e.getStatus()))
                    .count();
            averageProgress = enrollments.isEmpty() ? 0.0 : enrollments.stream()
                    .mapToDouble(e -> e.getCompletionPercentage() != null ? e.getCompletionPercentage() : 0.0)
                    .average()
                    .orElse(0.0);

            // Active students based on real lesson activity
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            Set<Long> enrollmentIds = enrollments.stream()
                    .map(Enrollment::getId)
                    .collect(Collectors.toSet());
            List<Long> recentIds7 = lessonProgressRepository.findEnrollmentIdsWithActivitySince(sevenDaysAgo);
            List<Long> recentIds30 = lessonProgressRepository.findEnrollmentIdsWithActivitySince(thirtyDaysAgo);
            activeStudents7 = (int) recentIds7.stream().filter(enrollmentIds::contains).count();
            activeStudents30 = (int) recentIds30.stream().filter(enrollmentIds::contains).count();

            final List<Enrollment> finalEnrollments = enrollments;
            totalRevenue = finalEnrollments.stream()
                    .filter(e -> e.getPaymentIntentId() != null && !e.getPaymentIntentId().isBlank())
                    .mapToDouble(e -> course.getPrice() != null ? course.getPrice().doubleValue() : 0.0)
                    .sum();
            LocalDateTime firstOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            monthlyRevenue = finalEnrollments.stream()
                    .filter(e -> e.getPaymentIntentId() != null
                            && !e.getPaymentIntentId().isBlank()
                            && e.getEnrollmentDate() != null
                            && e.getEnrollmentDate().isAfter(firstOfMonth))
                    .mapToDouble(e -> course.getPrice() != null ? course.getPrice().doubleValue() : 0.0)
                    .sum();
        } catch (Exception e) {
            // Enrollment data unavailable — return zeroed metrics
            totalEnrollments = 0;
            completedStudents = 0;
            averageProgress = 0.0;
            totalRevenue = 0.0;
            monthlyRevenue = 0.0;
        }

        // Calculate total time spent across all lesson progress records for this course
        long totalTimeSpentSeconds = 0L;
        String totalTimeSpentFormatted = "0m";
        try {
            List<Long> enrollmentIdList = enrollments.stream()
                    .map(Enrollment::getId)
                    .collect(Collectors.toList());
            if (!enrollmentIdList.isEmpty()) {
                List<tn.esprit.tpfoyer.Entities.LessonProgress> allProgress =
                        lessonProgressRepository.findByEnrollmentIdIn(enrollmentIdList);
                totalTimeSpentSeconds = allProgress.stream()
                        .mapToLong(lp -> lp.getTimeSpentSeconds() != null ? lp.getTimeSpentSeconds() : 0)
                        .sum();
                long hours = totalTimeSpentSeconds / 3600;
                long minutes = (totalTimeSpentSeconds % 3600) / 60;
                totalTimeSpentFormatted = hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
            }
        } catch (Exception ignored) {}

        // Build analytics DTO
        return CourseAnalyticsDTO.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .totalEnrollments(totalEnrollments)
                .activeStudentsLast7Days(activeStudents7)
                .activeStudentsLast30Days(activeStudents30)
                .totalLessons(totalLessons)
                .averageProgressPercentage(averageProgress)
                .completedStudents(completedStudents)
                .averageRating(course.getRating() != null ? course.getRating().doubleValue() : 0.0)
                .totalRatings(course.getRatingCount() != null ? course.getRatingCount() : 0)
                .totalReviews((int) courseReviewRepository.countByCourseId(courseId))
                .totalVideoMinutes(totalVideoMinutes)
                .totalPdfMaterials(totalPdfMaterials)
                .totalImageMaterials(totalImageMaterials)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .totalTimeSpentSeconds(totalTimeSpentSeconds)
                .totalTimeSpentFormatted(totalTimeSpentFormatted)
                .lastUpdated(course.getUpdatedAt() != null ? course.getUpdatedAt().toString() : null)
                .build();
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
                .instructorId(course.getInstructorId())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    /**
     * Overloaded method that uses pre-fetched lesson counts to avoid N+1 queries.
     * Used when mapping a list of courses.
     */
    private CourseResponseDTO toResponseDTO(Course course, Map<Long, Integer> lessonCountMap) {
        int lessonCount = lessonCountMap.getOrDefault(course.getId(), 0);
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
                .lessonCount(lessonCount)
                .instructorId(course.getInstructorId())
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
                .instructorId(base.getInstructorId())
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .lessons(lessons)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AiSearchResultDTO aiSearch(String query, int page, int size) {
        AiSearchIntentDTO intent = aiSearchService.extractSearchIntent(query);

        // fall back to original query if keywords blank
        String keywords = (intent.getKeywords() != null && !intent.getKeywords().isBlank())
                ? intent.getKeywords()
                : query;

        CourseCategory category = null;
        if (intent.getCategory() != null && !intent.getCategory().equalsIgnoreCase("null")) {
            try {
                category = CourseCategory.valueOf(intent.getCategory().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        CourseLevel level = null;
        if (intent.getLevel() != null && !intent.getLevel().equalsIgnoreCase("null")) {
            try {
                level = CourseLevel.valueOf(intent.getLevel().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        log.info("[AI Search] query='{}' → category={} level={} keywords='{}'",
                query, category, level, keywords);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // STEP 1 — Try with all filters (keywords + category + level)
        Page<CourseResponseDTO> results = getAllCourses(
                keywords, category, level, null, null, null, null, pageable);
        log.info("[AI Search] STEP1 results: {}", results.getTotalElements());

        // STEP 2 — If empty and level was set, try without level
        if (results.isEmpty() && level != null) {
            results = getAllCourses(
                    keywords, category, null, null, null, null, null, pageable);
            log.info("[AI Search] STEP2 results: {}", results.getTotalElements());
        }

        // STEP 3 — If still empty and category was set, try keyword only (no category/level)
        if (results.isEmpty() && category != null) {
            results = getAllCourses(
                    keywords, null, null, null, null, null, null, pageable);
            log.info("[AI Search] STEP3 results: {}", results.getTotalElements());
        }

        // STEP 4 — If still empty and category was set, try category only (no keyword)
        if (results.isEmpty() && category != null) {
            results = getAllCourses(
                    null, category, null, null, null, null, null, pageable);
            log.info("[AI Search] STEP4 (category-only) results: {}", results.getTotalElements());
        }

        // STEP 5 — Last resort: raw query with no filters
        if (results.isEmpty()) {
            results = getAllCourses(
                    query, null, null, null, null, null, null, pageable);
            log.info("[AI Search] STEP5 results: {}", results.getTotalElements());
        }

        log.info("[AI Search] found {} candidates", results.getTotalElements());

        // ── Build mutable list for merging ───────────────────────────────────
        List<CourseResponseDTO> merged = new ArrayList<>(results.getContent());
        Set<Long> seenIds = merged.stream()
                .map(CourseResponseDTO::getId)
                .collect(Collectors.toSet());

        // FALLBACK ENRICHMENT — If < 3 results and a category was detected,
        // pad with category-only courses so the page is never nearly empty.
        if (merged.size() < 3 && category != null) {
            getAllCourses(null, category, null, null, null, null, null, pageable)
                    .getContent().stream()
                    .filter(c -> !seenIds.contains(c.getId()))
                    .forEach(c -> { merged.add(c); seenIds.add(c.getId()); });
            log.info("[AI Search] After category-only enrichment: {} candidates", merged.size());
        }

        // LESSON-TITLE SEARCH — find courses whose lesson titles match any keyword term
        // and merge them in (up to the requested page size).
        String[] terms = keywords.split("[,\\s]+");
        for (String term : terms) {
            String t = term.trim();
            if (t.isBlank() || merged.size() >= size) continue;
            List<Long> courseIdsFromLessons = lessonRepository
                    .findByTitleContainingIgnoreCase(t)
                    .stream()
                    .map(l -> l.getCourse().getId())
                    .distinct()
                    .filter(id -> !seenIds.contains(id))
                    .collect(Collectors.toList());
            for (Long courseId : courseIdsFromLessons) {
                if (merged.size() >= size) break;
                courseRepository.findById(courseId).ifPresent(course -> {
                    merged.add(toResponseDTO(course));
                    seenIds.add(course.getId());
                });
            }
        }
        log.info("[AI Search] Final result count after lesson-title merge: {}", merged.size());

        // Cap at requested page size and wrap in a Page
        List<CourseResponseDTO> capped = merged.stream().limit(size).collect(Collectors.toList());
        Page<CourseResponseDTO> finalPage = new PageImpl<>(
                capped,
                pageable,
                Math.max(results.getTotalElements(), capped.size())
        );

        return new AiSearchResultDTO(
                intent.getExplanation(),
                keywords,
                intent.getCategory(),
                intent.getLevel(),
                finalPage
        );
    }

    private void validateOwnership(Course course, Long requestingUserId, String requestingRole) {
        if (requestingRole != null && requestingRole.equalsIgnoreCase("ADMIN")) {
            return; // Admins can modify any course
        }

        if (course.getInstructorId() == null) {
            return; // Course has no owner, allow modification
        }

        if (requestingUserId == null || !course.getInstructorId().equals(requestingUserId)) {
            throw new IllegalArgumentException(
                "Access denied: You can only modify courses you created"
            );
        }
    }
}
