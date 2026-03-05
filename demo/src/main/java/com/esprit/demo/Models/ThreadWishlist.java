package com.esprit.demo.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "thread_wishlists",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "thread_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ThreadWishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;

    @Column(name = "saved_at")
    @Builder.Default
    private LocalDateTime savedAt = LocalDateTime.now();
}
