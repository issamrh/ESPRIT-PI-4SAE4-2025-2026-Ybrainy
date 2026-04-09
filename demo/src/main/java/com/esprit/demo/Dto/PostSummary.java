package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSummary {
    private Long id;
    private String title;
    private long upvotes;
    private long totalReactions;
}
