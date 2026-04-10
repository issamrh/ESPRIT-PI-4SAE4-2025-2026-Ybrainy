package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;

    private Long senderId;
    private String senderUsername;
    private String senderRole;
    private Integer senderLevel;
    private String senderLevelTitle;

    private Long receiverId;
    private String receiverUsername;

    private String content;
    private String mediaUrl;
    private String mediaType;

    private boolean read;
    private LocalDateTime createdAt;
}
