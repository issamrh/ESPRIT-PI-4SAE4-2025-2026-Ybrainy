package com.esprit.demo.Mappers;

import com.esprit.demo.Dto.AuthorResponse;
import com.esprit.demo.Dto.CommentResponse;
import com.esprit.demo.Models.Comment;
import com.esprit.demo.Models.LevelConfig;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        AuthorResponse author = AuthorResponse.builder()
                .id(comment.getAuthor().getId())
                .username(comment.getAuthor().getUsername())
                .email(comment.getAuthor().getEmail())
                .level(comment.getAuthor().getLevel())
                .levelTitle(LevelConfig.getTitle(comment.getAuthor().getLevel()))
                .xp(comment.getAuthor().getXp())
                .build();

        return CommentResponse.builder()
                .id(comment.getId())
                .body(comment.getBody())
                .author(author)
                .postId(comment.getPost().getId())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
