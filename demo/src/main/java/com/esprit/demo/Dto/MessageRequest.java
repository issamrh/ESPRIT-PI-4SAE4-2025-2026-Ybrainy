package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private Long senderId;
    private Long receiverId;
    private String content;
    private String mediaUrl;
    private String mediaType;
}
