package com.esprit.demo.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String username; // accepts username OR email

    @NotBlank
    private String password;
}
