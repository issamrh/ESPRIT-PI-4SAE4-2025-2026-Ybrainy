package com.esprit.demo.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private String message;
    private Long threadId;
    private boolean read;
    private LocalDateTime createdAt;
}
