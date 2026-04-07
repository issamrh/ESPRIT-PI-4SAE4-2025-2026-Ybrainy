package tn.esprit.tpfoyer.Controllers;

import jakarta.validation.Valid;
import tn.esprit.tpfoyer.Dto.CourseProgressDTO;
import tn.esprit.tpfoyer.Dto.EnrollmentCreateDTO;
import tn.esprit.tpfoyer.Dto.EnrollmentDTO;
import tn.esprit.tpfoyer.Dto.StudentDashboardDTO;
import tn.esprit.tpfoyer.Entities.Enrollment;
import tn.esprit.tpfoyer.Repositories.EnrollmentRepository;
import tn.esprit.tpfoyer.Services.IEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class EnrollmentController {

    private final IEnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;

    // POST /api/enrollments
    @PostMapping("/api/enrollments")
    public ResponseEntity<?> enroll(@RequestBody @Valid EnrollmentCreateDTO body) {
        Long studentId = body.getStudentId();
        Long courseId  = body.getCourseId();
        try {
            EnrollmentDTO dto = enrollmentService.enrollStudent(studentId, courseId);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Already enrolled")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", e.getMessage()));
            }
            throw e;
        }
    }

    // GET /api/enrollments/student/{studentId}
    @GetMapping("/api/enrollments/student/{studentId}")
    public ResponseEntity<List<EnrollmentDTO>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId));
    }

    // GET /api/enrollments/check?studentId=&courseId=
    @GetMapping("/api/enrollments/check")
    public ResponseEntity<Map<String, Boolean>> checkEnrollment(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);
        return ResponseEntity.ok(Map.of("enrolled", enrolled));
    }

    // GET /api/courses/{courseId}/progress?studentId=
    @GetMapping("/api/courses/{courseId}/progress")
    public ResponseEntity<CourseProgressDTO> getCourseProgress(
            @PathVariable Long courseId,
            @RequestParam Long studentId) {
        return ResponseEntity.ok(enrollmentService.getCourseProgress(courseId, studentId));
    }

    // POST /api/courses/{courseId}/lessons/{lessonId}/complete?studentId=
    @PostMapping("/api/courses/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<CourseProgressDTO> markLessonComplete(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestParam Long studentId) {
        return ResponseEntity.ok(enrollmentService.markLessonComplete(courseId, lessonId, studentId));
    }

    // PATCH /api/courses/{courseId}/lessons/{lessonId}/time?studentId=&seconds=
    @PatchMapping("/api/courses/{courseId}/lessons/{lessonId}/time")
    public ResponseEntity<Void> trackTime(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestParam Long studentId,
            @RequestParam Integer seconds) {
        try {
            enrollmentService.trackTimeSpent(courseId, lessonId, studentId, seconds);
        } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    // GET /api/students/{studentId}/dashboard
    @GetMapping("/api/students/{studentId}/dashboard")
    public ResponseEntity<StudentDashboardDTO> getStudentDashboard(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getStudentDashboard(studentId));
    }

    // GET /api/enrollments/monthly-counts
    @GetMapping("/api/enrollments/monthly-counts")
    public ResponseEntity<?> getMonthlyEnrollmentCounts() {
        try {
            List<Enrollment> all = enrollmentRepository.findAll();
            Map<String, Long> monthlyCounts = all.stream()
                .filter(e -> e.getEnrollmentDate() != null)
                .collect(Collectors.groupingBy(
                    e -> e.getEnrollmentDate().toString().substring(0, 7),
                    Collectors.counting()
                ));
            List<Map<String, Object>> result = monthlyCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("month", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}
