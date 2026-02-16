package com.ybrainy.backend.entity;


import com.ybrainy.backend.entity.enums.CartStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long cartId;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private CartStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
