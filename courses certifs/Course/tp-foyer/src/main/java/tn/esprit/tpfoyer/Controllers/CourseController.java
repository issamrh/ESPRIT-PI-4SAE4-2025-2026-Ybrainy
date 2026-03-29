package tn.esprit.tpfoyer.Controllers;

import tn.esprit.tpfoyer.Dto.AiSearchRequestDTO;
import tn.esprit.tpfoyer.Dto.AiSearchResultDTO;
import tn.esprit.tpfoyer.Dto.CourseDetailResponseDTO;
import tn.esprit.tpfoyer.Dto.CourseRequestDTO;
import tn.esprit.tpfoyer.Dto.CourseResponseDTO;
import tn.esprit.tpfoyer.Dto.VerificationResponseDTO;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.enums.CourseCategory;
import tn.esprit.tpfoyer.Entities.enums.CourseLevel;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Services.ICertificateService;
import tn.esprit.tpfoyer.Services.ICourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final ICourseService courseService;
    private final ICertificateService certificateService;
    private final CourseRepository courseRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponseDTO> createCourse(
            @RequestPart("course") @Valid CourseRequestDTO dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(dto, thumbnail, certificate));
    }

    @GetMapping
    public ResponseEntity<Page<CourseResponseDTO>> getAllCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CourseCategory category,
            @RequestParam(required = false) CourseLevel level,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) Long instructorId,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                courseService.getAllCourses(search, category, level, minPrice, maxPrice, isPublished, instructorId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailResponseDTO> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponseDTO> updateCourse(
            @PathVariable Long id,
            @RequestPart("course") @Valid CourseRequestDTO dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate,
            @RequestParam(required = false) Long requestingUserId,
            @RequestParam(required = false) String requestingRole) {

        return ResponseEntity.ok(courseService.updateCourse(id, dto, thumbnail, certificate, requestingUserId, requestingRole));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id,
            @RequestParam(required = false) Long requestingUserId,
            @RequestParam(required = false) String requestingRole) {
        courseService.deleteCourse(id, requestingUserId, requestingRole);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<?> togglePublish(
            @PathVariable Long id,
            @RequestParam boolean publish,
            @RequestParam(required = false) Long requestingUserId,
            @RequestParam(required = false) String requestingRole) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found: " + id));

            // Authorization: ADMIN can publish any course,
            // INSTRUCTOR can only publish their own courses
            if ("INSTRUCTOR".equals(requestingRole) && requestingUserId != null) {
                if (course.getInstructorId() != null &&
                    !course.getInstructorId().equals(requestingUserId)) {
                    return ResponseEntity.status(403)
                        .body(Map.of("error", "You can only publish your own courses"));
                }
            }

            course.setIsPublished(publish);
            courseRepository.save(course);

            return ResponseEntity.ok(Map.of(
                "id", id,
                "isPublished", publish,
                "message", publish ? "Course published successfully" : "Course unpublished successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/search/ai")
    public ResponseEntity<?> aiSearch(
            @RequestBody AiSearchRequestDTO request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().body("Query must not be blank");
        }
        AiSearchResultDTO result = courseService.aiSearch(request.getQuery(), page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{courseId}/certificate")
    public ResponseEntity<?> downloadCertificate(
            @PathVariable Long courseId,
            @RequestParam Long studentId) {
        return certificateService.generateCertificate(courseId, studentId);
    }

    @GetMapping("/verify/{certificateId}")
    public ResponseEntity<VerificationResponseDTO> verifyCertificate(
            @PathVariable String certificateId) {
        return ResponseEntity.ok(certificateService.verifyCertificate(certificateId));
    }

    // GET /api/courses/stats/by-category
    @GetMapping("/stats/by-category")
    public ResponseEntity<?> getCourseStatsByCategory() {
        try {
            List<Course> all = courseRepository.findAll();
            Map<String, Long> catCounts = all.stream()
                .filter(c -> c.getCategory() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getCategory().name(),
                    Collectors.counting()
                ));
            return ResponseEntity.ok(catCounts);
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<>());
        }
    }
}
