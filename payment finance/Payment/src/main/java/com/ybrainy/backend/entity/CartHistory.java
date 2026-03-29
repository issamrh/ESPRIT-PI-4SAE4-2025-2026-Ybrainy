package com.ybrainy.backend.entity;

import com.ybrainy.backend.entity.enums.CartAction;
import com.ybrainy.backend.entity.enums.CartStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long cartId;

    private Long cartItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CartAction action;

    private String packTitle;

    private Integer quantity;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private CartStatus cartStatus;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
