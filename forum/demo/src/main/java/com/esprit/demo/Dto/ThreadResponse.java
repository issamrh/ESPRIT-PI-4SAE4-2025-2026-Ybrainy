package com.esprit.demo.Dto;

import com.esprit.demo.Models.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadResponse {

    private Long id;
    private String title;
    private String body;
    private String imageUrl;
    private String fileUrl;
    private String fileType;
    private Status status;
    private AuthorResponse author;
    private CategoryResponse category;
    private LocalDateTime createdAt;

    @Builder.Default
    private long upvoteCount = 0;
    @Builder.Default
    private long downvoteCount = 0;
    @Builder.Default
    private long voteScore = 0;
    @Builder.Default
    private long likeCount = 0;
    @Builder.Default
    private long dislikeCount = 0;
}
