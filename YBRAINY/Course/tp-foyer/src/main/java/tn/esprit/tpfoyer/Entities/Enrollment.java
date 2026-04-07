package tn.esprit.tpfoyer.Entities;

import tn.esprit.tpfoyer.Entities.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments", indexes = {
        @Index(name = "idx_enrollment_student",        columnList = "student_id"),
        @Index(name = "idx_enrollment_course",         columnList = "course_id"),
        @Index(name = "idx_enrollment_student_course", columnList = "student_id, course_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @CreationTimestamp
    @Column(name = "enrollment_date", updatable = false)
    private LocalDateTime enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "current_lesson_id")
    private Long currentLessonId;

    @Column(name = "completion_percentage", nullable = false)
    @Builder.Default
    private Double completionPercentage = 0.0;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "certificate_id", unique = true)
    private String certificateId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
