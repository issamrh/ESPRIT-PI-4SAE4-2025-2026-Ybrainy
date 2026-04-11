package tn.esprit.tpfoyer.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tpfoyer.Dto.MlQualityDTO;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.Enrollment;
import tn.esprit.tpfoyer.Entities.Lesson;
import tn.esprit.tpfoyer.Entities.LessonProgress;
import tn.esprit.tpfoyer.Entities.enums.ProgressStatus;
import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Repositories.EnrollmentRepository;
import tn.esprit.tpfoyer.Repositories.LessonProgressRepository;
import tn.esprit.tpfoyer.Repositories.LessonRepository;
import tn.esprit.tpfoyer.Services.MLServiceClient;

import tn.esprit.tpfoyer.Dto.MlConversionDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MLController {

    private final MLServiceClient mlClient;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    // GET /api/ml/recommendations?category=PROGRAMMING&level=BEGINNER&topN=5
    // OR  /api/ml/recommendations?studentId=1&topN=5  (derives category/level from enrollments)
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "BEGINNER") String level,
            @RequestParam(defaultValue = "5") int topN,
            @RequestParam(required = false) Long studentId) {

        List<Long> enrolledCourseIds = new ArrayList<>();

        if (studentId != null) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

            enrolledCourseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

            List<Course> courses = courseRepository.findAllById(enrolledCourseIds);

            // Find most frequent category from all enrollments
            Map<String, Long> categoryCounts = courses.stream()
                .filter(c -> c.getCategory() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getCategory().name(),
                    Collectors.counting()
                ));

            String dominantCategory = categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("PROGRAMMING");

            // Find most frequent level from all enrollments
            Map<String, Long> levelCounts = courses.stream()
                .filter(c -> c.getLevel() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getLevel().name(),
                    Collectors.counting()
                ));

            String dominantLevel = levelCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("BEGINNER");

            category = dominantCategory;
            level = dominantLevel;
        }

        if (category == null || category.isBlank()) {
            category = "PROGRAMMING";
        }

        return ResponseEntity.ok(mlClient.getRecommendations(category, level, topN, enrolledCourseIds));
    }

    // GET /api/ml/student/{studentId}/conversion
    // Uses real student data from DB to predict conversion
    @GetMapping("/student/{studentId}/conversion")
    public ResponseEntity<?> getConversionPrediction(@PathVariable Long studentId) {
        try {
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

            double completionRate = enrollments.stream()
                    .mapToDouble(e -> e.getCompletionPercentage() != null
                            ? e.getCompletionPercentage() : 0.0)
                    .average().orElse(0.0) / 100.0;

            List<LessonProgress> allProgress = new ArrayList<>();
            for (Enrollment e : enrollments) {
                allProgress.addAll(lessonProgressRepository.findByEnrollmentId(e.getId()));
            }

            double timeSpent = allProgress.stream()
                    .mapToDouble(lp -> lp.getTimeSpentSeconds() != null
                            ? lp.getTimeSpentSeconds() / 60.0 : 0.0)
                    .sum();

            long videosWatched = allProgress.stream()
                    .filter(lp -> ProgressStatus.COMPLETED.equals(lp.getStatus()))
                    .count();

            long paidEnrollments = enrollments.stream()
                    .filter(e -> e.getPaymentIntentId() != null
                            && !e.getPaymentIntentId().isBlank())
                    .count();

            // Fetch real quiz score from Quiz service
            double quizScores = 50.0; // default fallback
            try {
                org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> quizData = rt.getForObject(
                    "http://localhost:8083/api/quizzes/student/" + studentId + "/avg-score",
                    java.util.Map.class);
                if (quizData != null && quizData.get("avgScore") != null) {
                    quizScores = ((Number) quizData.get("avgScore")).doubleValue();
                }
            } catch (Exception ignored) {
                // Quiz service unavailable — use default 50.0
            }

            // Use total lesson progress records as proxy for platform engagement
            int lessonInteractions = allProgress.size();

            var result = mlClient.predictConversion(
                    timeSpent,
                    completionRate,
                    quizScores,
                    (double) videosWatched,
                    (double) lessonInteractions,
                    0.0);

            Map<String, Object> response = new HashMap<>();
            response.put("conversionProbability", result.getConversionProbability());
            response.put("conversionLabel", result.getConversionLabel());
            response.put("percentage", result.getPercentage());
            response.put("totalEnrollments", enrollments.size());
            response.put("paidEnrollments", paidEnrollments);
            response.put("avgCompletionRate", Math.round(completionRate * 100));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "conversionProbability", 0.5,
                    "conversionLabel", "MEDIUM",
                    "percentage", 50.0));
        }
    }

    // GET /api/ml/course/{courseId}/quality
    // Uses real course data to predict quality
    @GetMapping("/course/{courseId}/quality")
    public ResponseEntity<?> getCourseQuality(@PathVariable Long courseId) {
        try {
            System.out.println("[ML Quality] courseId=" + courseId);
            Course course = courseRepository.findById(courseId).orElseThrow();
            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
            System.out.println("[ML Quality] lessons count=" + lessons.size());

            int numLessons = lessons.size();
            int numLectures = numLessons;

            double contentDuration = lessons.stream()
                    .mapToDouble(l -> l.getDurationMinutes() != null
                            ? l.getDurationMinutes() : 0)
                    .sum() / 60.0;

            long distinctTypes = lessons.stream()
                    .filter(l -> l.getType() != null)
                    .map(l -> l.getType().name())
                    .distinct().count();

            long videoLessons = lessons.stream()
                    .filter(l -> l.getType() != null &&
                            (l.getType().name().equals("VIDEO_UPLOAD") ||
                             l.getType().name().equals("YOUTUBE_EMBED")))
                    .count();

            double pctVideo = numLessons > 0 ? (double) videoLessons / numLessons : 0.0;

            int certEncoded = Boolean.TRUE.equals(course.getOffersCertificate()) ? 1 : 0;

            int levelEncoded = 0;
            if (course.getLevel() != null) {
                switch (course.getLevel()) {
                    case INTERMEDIATE -> levelEncoded = 1;
                    case ADVANCED -> levelEncoded = 2;
                    default -> levelEncoded = 0;
                }
            }

            System.out.println("[ML Quality] calling Flask with numLectures=" + numLectures
                    + " contentDuration=" + contentDuration
                    + " distinctTypes=" + distinctTypes
                    + " pctVideo=" + pctVideo
                    + " numLessons=" + numLessons
                    + " certEncoded=" + certEncoded
                    + " levelEncoded=" + levelEncoded);

            double rating = course.getRating() != null ? course.getRating() : 0.0;
            int ratingCount = course.getRatingCount() != null ? course.getRatingCount() : 0;

            MlQualityDTO quality = mlClient.predictQuality(
                    numLectures, contentDuration,
                    (int) distinctTypes, pctVideo,
                    numLessons, certEncoded, levelEncoded,
                    rating, ratingCount);

            System.out.println("[ML Quality] Flask response: " + quality);
            return ResponseEntity.ok(quality);
        } catch (Exception e) {
            System.out.println("[ML Quality] Exception for course " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new MlQualityDTO("UNKNOWN", 0.0, 0.0, false, null, null, null));
        }
    }

    // POST /api/ml/courses/quality-batch
    // Batch quality prediction for a list of course IDs
    @PostMapping("/courses/quality-batch")
    public ResponseEntity<?> getCourseQualityBatch(
            @RequestBody List<Long> courseIds) {
        Map<Long, MlQualityDTO> results = new HashMap<>();
        for (Long courseId : courseIds) {
            try {
                Course course = courseRepository.findById(courseId).orElse(null);
                if (course == null) continue;

                List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
                int numLessons = lessons.size();
                int numLectures = numLessons;

                double contentDuration = lessons.stream()
                        .mapToDouble(l -> l.getDurationMinutes() != null
                                ? l.getDurationMinutes() : 0)
                        .sum() / 60.0;

                long distinctTypes = lessons.stream()
                        .filter(l -> l.getType() != null)
                        .map(l -> l.getType().name())
                        .distinct().count();

                long videoLessons = lessons.stream()
                        .filter(l -> l.getType() != null &&
                                (l.getType().name().equals("VIDEO_UPLOAD") ||
                                 l.getType().name().equals("YOUTUBE_EMBED")))
                        .count();

                double pctVideo = numLessons > 0 ? (double) videoLessons / numLessons : 0.0;

                int certEncoded = Boolean.TRUE.equals(course.getOffersCertificate()) ? 1 : 0;

                int levelEncoded = 0;
                if (course.getLevel() != null) {
                    switch (course.getLevel()) {
                        case INTERMEDIATE -> levelEncoded = 1;
                        case ADVANCED -> levelEncoded = 2;
                        default -> levelEncoded = 0;
                    }
                }

                double rating = course.getRating() != null ? course.getRating() : 0.0;
                int ratingCount = course.getRatingCount() != null ? course.getRatingCount() : 0;

                MlQualityDTO quality = mlClient.predictQuality(
                        numLectures, contentDuration,
                        (int) distinctTypes, pctVideo,
                        numLessons, certEncoded, levelEncoded,
                        rating, ratingCount);
                results.put(courseId, quality);
            } catch (Exception e) {
                System.out.println("[ML Quality Batch] Skipping course " + courseId + ": " + e.getMessage());
            }
        }
        return ResponseEntity.ok(results);
    }

    // GET /api/ml/forecast?steps=6
    @GetMapping("/forecast")
    public ResponseEntity<?> getForecast(
            @RequestParam(defaultValue = "6") int steps) {
        return ResponseEntity.ok(mlClient.getForecast(steps));
    }

    // GET /api/ml/admin/conversion-stats
    // Aggregate conversion analytics across all students using DSO1
    @GetMapping("/admin/conversion-stats")
    public ResponseEntity<?> getAdminConversionStats() {
        try {
            // Step 1: Get all enrollments
            List<Enrollment> allEnrollments = enrollmentRepository.findAll();

            // Step 2: Group by studentId
            Map<Long, List<Enrollment>> byStudent = allEnrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getStudentId));

            int totalStudents = byStudent.size();

            // Step 3: Identify converted students (those with at least one paid enrollment)
            Set<Long> convertedStudentIds = byStudent.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                    .anyMatch(e -> e.getPaymentIntentId() != null
                        && !e.getPaymentIntentId().isEmpty()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

            int convertedStudents = convertedStudentIds.size();

            // Step 4: Free-only students
            Set<Long> freeOnlyStudentIds = byStudent.keySet().stream()
                .filter(sid -> !convertedStudentIds.contains(sid))
                .collect(Collectors.toSet());

            int freeOnlyStudents = freeOnlyStudentIds.size();

            // Step 5: Run DSO1 on free-only students and average the probability
            double totalProbability = 0.0;
            int highPotentialCount = 0;
            int processed = 0;

            for (Long studentId : freeOnlyStudentIds) {
                try {
                    List<Enrollment> studentEnrollments = byStudent.get(studentId);

                    List<Long> enrollmentIds = studentEnrollments.stream()
                        .map(Enrollment::getId)
                        .collect(Collectors.toList());

                    List<LessonProgress> allProgress = enrollmentIds.isEmpty()
                        ? Collections.emptyList()
                        : lessonProgressRepository.findByEnrollmentIdIn(enrollmentIds);

                    double timeSpent = allProgress.stream()
                        .mapToDouble(lp -> lp.getTimeSpentSeconds() != null
                            ? lp.getTimeSpentSeconds() / 60.0 : 0)
                        .sum();

                    double completionRate = studentEnrollments.stream()
                        .mapToDouble(e -> e.getCompletionPercentage() != null
                            ? e.getCompletionPercentage() / 100.0 : 0)
                        .average().orElse(0.0);

                    double quizScores = 50.0;
                    try {
                        org.springframework.web.client.RestTemplate rt =
                            new org.springframework.web.client.RestTemplate();
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> quizData = rt.getForObject(
                            "http://localhost:8083/api/quizzes/student/" + studentId + "/avg-score",
                            java.util.Map.class);
                        if (quizData != null && quizData.get("avgScore") != null) {
                            quizScores = ((Number) quizData.get("avgScore")).doubleValue();
                        }
                    } catch (Exception ignored) {}

                    long videosWatched = allProgress.stream()
                        .filter(lp -> ProgressStatus.COMPLETED.equals(lp.getStatus()))
                        .count();

                    long lessonInteractions = allProgress.size();

                    MlConversionDTO result = mlClient.predictConversion(
                        timeSpent, completionRate, quizScores,
                        (double) videosWatched, (double) lessonInteractions, 0.0);

                    if (result != null && result.getPercentage() != null) {
                        double prob = result.getPercentage();
                        totalProbability += prob;
                        if (prob >= 70.0) highPotentialCount++;
                        processed++;
                    }
                } catch (Exception e) {
                    System.out.println("[ConversionStats] Failed for student "
                        + studentId + ": " + e.getMessage());
                }
            }

            double avgConversionProbability = processed > 0
                ? totalProbability / processed : 0.0;

            double conversionRate = totalStudents > 0
                ? (convertedStudents * 100.0) / totalStudents : 0.0;

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalStudents", totalStudents);
            stats.put("convertedStudents", convertedStudents);
            stats.put("freeOnlyStudents", freeOnlyStudents);
            stats.put("conversionRate", Math.round(conversionRate * 10.0) / 10.0);
            stats.put("avgConversionProbability",
                Math.round(avgConversionProbability * 10.0) / 10.0);
            stats.put("highPotentialCount", highPotentialCount);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.out.println("[ConversionStats] Error: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
