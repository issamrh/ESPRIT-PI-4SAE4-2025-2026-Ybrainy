package tn.esprit.tpfoyer.Controllers;

import tn.esprit.tpfoyer.Repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentCertificates(@PathVariable Long studentId) {
        try {
            List<Map<String, Object>> certificates = enrollmentRepository
                .findByStudentId(studentId)
                .stream()
                .filter(e -> e.getCertificateId() != null)
                .map(e -> {
                    Map<String, Object> cert = new HashMap<>();
                    cert.put("courseId", e.getCourseId());
                    cert.put("studentId", studentId);
                    cert.put("certificateId", e.getCertificateId());
                    cert.put("completedAt", e.getCompletedAt());
                    cert.put("enrollmentId", e.getId());
                    return cert;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
