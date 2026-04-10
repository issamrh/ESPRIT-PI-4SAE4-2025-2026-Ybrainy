package com.esprit.demo.Mappers;

import com.esprit.demo.Dto.AuthorResponse;
import com.esprit.demo.Dto.PostResponse;
import com.esprit.demo.Models.LevelConfig;
import com.esprit.demo.Models.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post) {
        AuthorResponse author = AuthorResponse.builder()
                .id(post.getAuthor().getId())
                .username(post.getAuthor().getUsername())
                .email(post.getAuthor().getEmail())
                .level(post.getAuthor().getLevel())
                .levelTitle(LevelConfig.getTitle(post.getAuthor().getLevel()))
                .xp(post.getAuthor().getXp())
                .build();

        return PostResponse.builder()
                .id(post.getId())
                .body(post.getBody())
                .imageUrl(post.getImageUrl())
                .fileUrl(post.getFileUrl())
                .fileType(post.getFileType())
                .author(author)
                .threadId(post.getThread().getId())
                .threadTitle(post.getThread().getTitle())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
