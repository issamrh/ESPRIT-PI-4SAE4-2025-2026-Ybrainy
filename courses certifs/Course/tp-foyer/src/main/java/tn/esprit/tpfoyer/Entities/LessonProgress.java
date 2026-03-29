package tn.esprit.tpfoyer.Entities;

import tn.esprit.tpfoyer.Entities.enums.ProgressStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress", indexes = {
        @Index(name = "idx_progress_enrollment",        columnList = "enrollment_id"),
        @Index(name = "idx_progress_enrollment_lesson", columnList = "enrollment_id, lesson_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "time_spent_seconds", nullable = false)
    @Builder.Default
    private Integer timeSpentSeconds = 0;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
}
