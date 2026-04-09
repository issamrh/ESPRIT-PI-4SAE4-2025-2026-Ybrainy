package tn.esprit.tpfoyer.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.tpfoyer.Dto.VerificationResponseDTO;
import tn.esprit.tpfoyer.Entities.*;
import tn.esprit.tpfoyer.Repositories.*;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements ICertificateService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.key}")
    private String openRouterKey;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // ── Colors ────────────────────────────────────────────────────────
    private static final DeviceRgb PRIMARY_DARK_BLUE = new DeviceRgb(26, 54, 93);
    private static final DeviceRgb ACCENT_BLUE       = new DeviceRgb(43, 108, 176);
    private static final DeviceRgb GOLD              = new DeviceRgb(200, 169, 81);
    private static final DeviceRgb TEXT_GRAY         = new DeviceRgb(113, 128, 150);
    private static final DeviceRgb WHITE             = new DeviceRgb(255, 255, 255);

    // ── generateCertificate ───────────────────────────────────────────
    @Override
    public ResponseEntity<?> generateCertificate(Long courseId, Long studentId) {
        try {
            // Step 1 — Load data
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            Enrollment enrollment = enrollmentRepository
                    .findByStudentIdAndCourseId(studentId, courseId)
                    .orElseThrow(() -> new RuntimeException("Enrollment not found"));

            if (enrollment.getCompletionPercentage() == null || enrollment.getCompletionPercentage() < 100) {
                throw new RuntimeException("Course not yet completed");
            }

            // Hours spent from lesson progress
            List<LessonProgress> progresses = lessonProgressRepository.findByEnrollmentId(enrollment.getId());
            int totalSeconds = progresses.stream()
                    .mapToInt(lp -> lp.getTimeSpentSeconds() != null ? lp.getTimeSpentSeconds() : 0)
                    .sum();
            int hoursSpent = Math.round(totalSeconds / 3600f);

            // Best quiz score across all quizzes in this course
            Double quizScore = getBestQuizScore(studentId, courseId);

            // Step 2 — Generate certificate ID if not exists
            if (enrollment.getCertificateId() == null) {
                String certId = "YBRY-" +
                        LocalDate.now().getYear() + "-" +
                        UUID.randomUUID().toString().substring(0, 4).toUpperCase() + "-" +
                        UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                enrollment.setCertificateId(certId);
                enrollmentRepository.save(enrollment);
            }
            String certId = enrollment.getCertificateId();

            // Step 3 — Resolve real student name
            String studentName = resolveStudentName(studentId);

            // Step 4 — Generate AI remarks
            String aiRemarks = generateAiRemarks(
                    course.getTitle(),
                    course.getCategory().name(),
                    hoursSpent,
                    quizScore
            );

            // Step 5 — Generate PDF
            String completionDate = enrollment.getCompletedAt() != null
                    ? enrollment.getCompletedAt().toLocalDate()
                            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                    : enrollment.getEnrollmentDate() != null
                            ? enrollment.getEnrollmentDate().toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                            : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            String certDir = uploadDir + "/certificates/";
            new File(certDir).mkdirs();
            String filePath = certDir + "cert_" + studentId + "_" + courseId + ".pdf";

            // Return cached file if it already exists (skips AI call + PDF rebuild)
            File existingFile = new File(filePath);
            if (existingFile.exists() && existingFile.length() > 0) {
                log.info("[Certificate] Returning cached PDF for student {} course {}", studentId, courseId);
                Resource cachedResource = new FileSystemResource(existingFile);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"YBrainy-Certificate.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(cachedResource);
            }

            generatePdf(filePath, course, studentId, studentName, certId, hoursSpent, quizScore, aiRemarks, completionDate);

            Resource resource = new FileSystemResource(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"YBrainy-Certificate.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if ("Course not found".equals(msg) || "Enrollment not found".equals(msg)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }
            if ("Course not yet completed".equals(msg)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
            }
            log.error("Certificate generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Certificate generation failed: " + msg);
        } catch (Exception e) {
            log.error("Certificate generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Certificate generation failed: " + e.getMessage());
        }
    }

    // ── verifyCertificate ─────────────────────────────────────────────
    @Override
    public VerificationResponseDTO verifyCertificate(String certificateId) {
        Optional<Enrollment> opt = enrollmentRepository.findByCertificateId(certificateId);
        if (opt.isEmpty()) {
            return VerificationResponseDTO.builder()
                    .valid(false)
                    .certificateId(certificateId)
                    .build();
        }
        Enrollment enrollment = opt.get();
        Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);

        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollmentId(enrollment.getId());
        int totalSeconds = progresses.stream()
                .mapToInt(lp -> lp.getTimeSpentSeconds() != null ? lp.getTimeSpentSeconds() : 0)
                .sum();
        int hoursSpent = Math.round(totalSeconds / 3600f);

        Double quizScore = course != null ? getBestQuizScore(enrollment.getStudentId(), course.getId()) : null;

        String completionDate = enrollment.getCompletedAt() != null
                ? enrollment.getCompletedAt().toLocalDate()
                        .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                : enrollment.getEnrollmentDate() != null
                        ? enrollment.getEnrollmentDate().toLocalDate()
                                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                        : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        return VerificationResponseDTO.builder()
                .valid(true)
                .certificateId(certificateId)
                .studentName(resolveStudentName(enrollment.getStudentId()))
                .studentId(enrollment.getStudentId())
                .courseTitle(course != null ? course.getTitle() : "Unknown Course")
                .completionDate(completionDate)
                .quizScore(quizScore)
                .hoursSpent(hoursSpent)
                .issuedBy("YBrainy E-Learning Platform")
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Double getBestQuizScore(Long studentId, Long courseId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "http://localhost:8083/api/quizzes/best-score?studentId="
                            + studentId + "&courseId=" + courseId,
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object score = response.getBody().get("bestScore");
                if (score instanceof Number) {
                    double val = ((Number) score).doubleValue();
                    return val > 0 ? val : null;
                }
            }
        } catch (Exception e) {
            log.warn("[Certificate] Could not get quiz score for student {} course {}: {}",
                    studentId, courseId, e.getMessage());
        }
        return null;
    }

    private String resolveStudentName(Long studentId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "http://localhost:8088/api/users/internal/" + studentId,
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String firstName = (String) response.getBody().getOrDefault("firstName", "");
                String lastName  = (String) response.getBody().getOrDefault("lastName", "");
                String fullName  = (firstName + " " + lastName).trim();
                if (!fullName.isEmpty()) return fullName;
            }
        } catch (Exception e) {
            log.warn("[Certificate] Could not resolve name for student {}: {}", studentId, e.getMessage());
        }
        return "Student #" + studentId;
    }

    private String generateAiRemarks(String courseTitle, String category, int hoursSpent, Double quizScore) {
        try {
            String prompt = """
                    Write a professional 2-3 sentence certificate remark for a \
                    student who completed the course "%s" in the %s category. \
                    They spent %d hours learning and achieved a quiz score of %.1f%%. \
                    Be encouraging, specific to the subject, and professional. \
                    Return only the remark text, no quotes, no labels.
                    """.formatted(courseTitle, category, hoursSpent, quizScore != null ? quizScore : 0.0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openRouterKey);
            headers.set("HTTP-Referer", "http://localhost:4301");
            headers.set("X-Title", "YBrainy");

            Map<String, Object> body = Map.of(
                    "model", "stepfun/step-3.5-flash:free",
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://openrouter.ai/api/v1/chat/completions", request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText().trim();

        } catch (Exception e) {
            log.warn("AI remarks generation failed: {}", e.getMessage());
            return "This student has demonstrated dedication and commitment in completing this course successfully.";
        }
    }

    private void generatePdf(String filePath, Course course, Long studentId, String studentName,
                              String certId, int hoursSpent, Double quizScore,
                              String aiRemarks, String completionDate) throws Exception {

        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        PageSize landscape = PageSize.A4.rotate();
        Document doc = new Document(pdfDoc, landscape);
        doc.setMargins(0, 50, 0, 50);

        // ── PAGE 1 — Main Certificate (Landscape) ─────────────────────

        // Top banner: dark blue with YBrainy title
        Table topBanner = new Table(1).useAllAvailableWidth()
                .setBackgroundColor(PRIMARY_DARK_BLUE)
                .setMarginLeft(-50).setMarginRight(-50);
        topBanner.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("YBrainy")
                        .setFont(bold).setFontSize(30)
                        .setFontColor(WHITE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(0))
                .add(new Paragraph("E-LEARNING PLATFORM")
                        .setFont(regular).setFontSize(8)
                        .setFontColor(new DeviceRgb(190, 210, 240))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setCharacterSpacing(4f))
                .setPadding(16f));
        doc.add(topBanner);

        // Gold decorative line below header
        doc.add(new Paragraph(" ")
                .setFontSize(2)
                .setBorderBottom(new SolidBorder(GOLD, 2f))
                .setMarginLeft(-50).setMarginRight(-50));

        // Certificate title
        doc.add(new Paragraph("CERTIFICATE OF COMPLETION")
                .setFont(bold).setFontSize(20)
                .setFontColor(PRIMARY_DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(2f)
                .setMarginTop(18f));

        doc.add(new Paragraph("This certifies that")
                .setFont(italic).setFontSize(12)
                .setFontColor(TEXT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10f));

        doc.add(new Paragraph(studentName)
                .setFont(bold).setFontSize(26)
                .setFontColor(PRIMARY_DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("has successfully completed")
                .setFont(regular).setFontSize(12)
                .setFontColor(TEXT_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph(course.getTitle())
                .setFont(bold).setFontSize(18)
                .setFontColor(PRIMARY_DARK_BLUE)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER));

        // Category badge
        Paragraph badge = new Paragraph(course.getCategory().name())
                .setFont(bold).setFontSize(9)
                .setFontColor(WHITE)
                .setBackgroundColor(ACCENT_BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(4f)
                .setBorderRadius(new BorderRadius(6f))
                .setWidth(UnitValue.createPointValue(100f))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(6f);
        doc.add(badge);

        doc.add(new Paragraph(" ").setFontSize(6));

        // Gold divider before stats
        doc.add(new Paragraph(" ")
                .setFontSize(2)
                .setBorderBottom(new SolidBorder(GOLD, 1f))
                .setMarginLeft(30).setMarginRight(30));

        doc.add(new Paragraph(" ").setFontSize(4));

        // Stats row
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth();
        statsTable.addCell(createStatCell("COMPLETED", completionDate, bold));
        statsTable.addCell(createStatCell("QUIZ SCORE",
                quizScore != null ? String.format("%.1f%%", quizScore) : "N/A", bold));
        statsTable.addCell(createStatCell("HOURS SPENT", hoursSpent + "h", bold));
        doc.add(statsTable);

        doc.add(new Paragraph(" ").setFontSize(4));

        // Bottom gold line
        doc.add(new Paragraph(" ")
                .setFontSize(2)
                .setBorderBottom(new SolidBorder(GOLD, 1f))
                .setMarginLeft(-50).setMarginRight(-50));

        doc.add(new Paragraph(" ").setFontSize(4));

        // Footer: cert ID + verify URL + issuer
        doc.add(new Paragraph("Certificate ID: " + certId)
                .setFont(regular).setFontSize(8)
                .setFontColor(TEXT_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("Verify at: " + frontendUrl + "/verify/" + certId)
                .setFont(italic).setFontSize(8)
                .setFontColor(ACCENT_BLUE)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("Issued by YBrainy E-Learning Platform")
                .setFont(regular).setFontSize(8)
                .setFontColor(TEXT_GRAY)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginRight(0));

        // Bottom border band
        Table bottomBanner = new Table(1).useAllAvailableWidth()
                .setBackgroundColor(PRIMARY_DARK_BLUE)
                .setMarginLeft(-50).setMarginRight(-50);
        bottomBanner.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(" ").setFontSize(5))
                .setPadding(4f));
        doc.add(bottomBanner);

        // ── PAGE 2 — AI Remarks (Landscape) ───────────────────────────
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // Header band
        Table p2Header = new Table(1).useAllAvailableWidth()
                .setBackgroundColor(PRIMARY_DARK_BLUE)
                .setMarginLeft(-50).setMarginRight(-50);
        p2Header.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("ACADEMIC ASSESSMENT")
                        .setFont(bold).setFontSize(18)
                        .setFontColor(WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setPadding(20f));
        doc.add(p2Header);

        // Gold line
        doc.add(new Paragraph(" ")
                .setFontSize(2)
                .setBorderBottom(new SolidBorder(GOLD, 2f))
                .setMarginLeft(-50).setMarginRight(-50));

        doc.add(new Paragraph(" ").setFontSize(10));

        doc.add(new Paragraph("Personal Remarks")
                .setFont(bold).setFontSize(18)
                .setFontColor(PRIMARY_DARK_BLUE));

        doc.add(new Paragraph(course.getTitle() + "  ·  " + studentName)
                .setFont(italic).setFontSize(11)
                .setFontColor(TEXT_GRAY));

        doc.add(new Paragraph(" ").setFontSize(8));

        // Opening quote
        doc.add(new Paragraph("\u201C")
                .setFont(bold).setFontSize(48)
                .setFontColor(GOLD)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(-16f));

        // AI remarks text
        doc.add(new Paragraph(aiRemarks)
                .setFont(regular).setFontSize(13)
                .setFontColor(TEXT_GRAY)
                .setMultipliedLeading(1.6f)
                .setMarginLeft(30f)
                .setMarginRight(30f));

        // Closing quote
        doc.add(new Paragraph("\u201D")
                .setFont(bold).setFontSize(48)
                .setFontColor(GOLD)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(-16f));

        doc.add(new Paragraph(" ").setFontSize(10));

        // Footer divider
        doc.add(new Paragraph(" ")
                .setFontSize(2)
                .setBorderBottom(new SolidBorder(GOLD, 1f))
                .setMarginLeft(-50).setMarginRight(-50));

        doc.add(new Paragraph(" ").setFontSize(6));

        doc.add(new Paragraph("Certificate ID: " + certId)
                .setFont(regular).setFontSize(9)
                .setFontColor(TEXT_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("YBrainy Academic Committee")
                .setFont(bold).setFontSize(10)
                .setFontColor(PRIMARY_DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8f));

        doc.close();
    }

    private Cell createStatCell(String label, String value, PdfFont bold) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(label)
                        .setFont(bold).setFontSize(8)
                        .setFontColor(TEXT_GRAY)
                        .setCharacterSpacing(1f)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph(value)
                        .setFont(bold).setFontSize(13)
                        .setFontColor(PRIMARY_DARK_BLUE)
                        .setTextAlignment(TextAlignment.CENTER));
    }
}