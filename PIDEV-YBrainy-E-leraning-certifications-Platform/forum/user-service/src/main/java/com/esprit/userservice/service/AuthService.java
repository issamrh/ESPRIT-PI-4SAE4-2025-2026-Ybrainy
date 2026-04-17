package com.esprit.userservice.service;

import com.esprit.userservice.dto.AuthResponse;
import com.esprit.userservice.dto.LoginRequest;
import com.esprit.userservice.exception.BadRequestException;
import com.esprit.userservice.model.User;
import com.esprit.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {
        User user;

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        } else if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        } else {
            throw new BadRequestException("Username or email is required");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }

        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(String.valueOf(user.getRole()))
                .xp((int) user.getXp())
                .level(user.getLevel())
                .build();
    }
}
