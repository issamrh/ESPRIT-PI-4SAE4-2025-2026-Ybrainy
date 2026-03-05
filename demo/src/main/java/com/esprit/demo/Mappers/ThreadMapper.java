package com.esprit.demo.Mappers;

import com.esprit.demo.Dto.AuthorResponse;
import com.esprit.demo.Dto.CategoryResponse;
import com.esprit.demo.Dto.ThreadResponse;
import com.esprit.demo.Models.LevelConfig;
import com.esprit.demo.Models.Thread;
import org.springframework.stereotype.Component;

@Component
public class ThreadMapper {

    public ThreadResponse toResponse(Thread thread) {
        AuthorResponse author = AuthorResponse.builder()
                .id(thread.getAuthor().getId())
                .username(thread.getAuthor().getUsername())
                .email(thread.getAuthor().getEmail())
                .level(thread.getAuthor().getLevel())
                .levelTitle(LevelConfig.getTitle(thread.getAuthor().getLevel()))
                .xp(thread.getAuthor().getXp())
                .build();

        CategoryResponse category = null;
        if (thread.getCategory() != null) {
            category = CategoryResponse.builder()
                    .id(thread.getCategory().getId())
                    .name(thread.getCategory().getName())
                    .description(thread.getCategory().getDescription())
                    .build();
        }

        return ThreadResponse.builder()
                .id(thread.getId())
                .title(thread.getTitle())
                .body(thread.getBody())
                .imageUrl(thread.getImageUrl())
                .fileUrl(thread.getFileUrl())
                .fileType(thread.getFileType())
                .status(thread.getStatus())
                .author(author)
                .category(category)
                .createdAt(thread.getCreatedAt())
                .build();
    }

    public ThreadResponse toResponse(Thread thread, long upvotes, long downvotes, long likes, long dislikes) {
        AuthorResponse author = AuthorResponse.builder()
                .id(thread.getAuthor().getId())
                .username(thread.getAuthor().getUsername())
                .email(thread.getAuthor().getEmail())
                .level(thread.getAuthor().getLevel())
                .levelTitle(LevelConfig.getTitle(thread.getAuthor().getLevel()))
                .xp(thread.getAuthor().getXp())
                .build();

        CategoryResponse category = null;
        if (thread.getCategory() != null) {
            category = CategoryResponse.builder()
                    .id(thread.getCategory().getId())
                    .name(thread.getCategory().getName())
                    .description(thread.getCategory().getDescription())
                    .build();
        }

        return ThreadResponse.builder()
                .id(thread.getId())
                .title(thread.getTitle())
                .body(thread.getBody())
                .imageUrl(thread.getImageUrl())
                .fileUrl(thread.getFileUrl())
                .fileType(thread.getFileType())
                .status(thread.getStatus())
                .author(author)
                .category(category)
                .createdAt(thread.getCreatedAt())
                .upvoteCount(upvotes)
                .downvoteCount(downvotes)
                .voteScore(upvotes - downvotes)
                .likeCount(likes)
                .dislikeCount(dislikes)
                .build();
    }
}
