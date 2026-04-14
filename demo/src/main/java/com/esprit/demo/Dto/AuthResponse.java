package com.esprit.demo.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private long xp;
    private int level;
}
