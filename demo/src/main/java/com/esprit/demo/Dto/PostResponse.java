package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String body;
    private String imageUrl;
    private String fileUrl;
    private String fileType;
    private AuthorResponse author;
    private Long threadId;
    private String threadTitle;
    private LocalDateTime createdAt;
}
