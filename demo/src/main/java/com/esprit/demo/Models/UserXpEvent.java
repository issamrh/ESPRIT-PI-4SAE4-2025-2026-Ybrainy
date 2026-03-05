package com.esprit.demo.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_xp_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserXpEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private XpSource sourceType;

    @Column(nullable = false)
    private int amount;

    /** ID de l'entité qui a déclenché cet XP (thread/post/comment id) */
    private Long referenceId;

    /** XP total de l'utilisateur APRÈS cet événement */
    @Column(nullable = false)
    private long newTotal;

    /** Niveau de l'utilisateur APRÈS cet événement */
    @Column(nullable = false)
    private int newLevel;

    /** Description lisible : "+20 XP — Thread créé" */
    private String description;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** True si cet événement a provoqué un level-up */
    public boolean isLevelUp() {
        int levelBefore = LevelConfig.computeLevel(newTotal - amount);
        return newLevel > levelBefore;
    }
}
