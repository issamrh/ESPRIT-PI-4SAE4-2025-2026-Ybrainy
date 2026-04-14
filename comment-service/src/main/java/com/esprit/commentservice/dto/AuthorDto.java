package com.esprit.commentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
    private int level;
    private String levelTitle;
}
