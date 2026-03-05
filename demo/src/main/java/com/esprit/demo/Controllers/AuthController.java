package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.AuthResponse;
import com.esprit.demo.Dto.LoginRequest;
import com.esprit.demo.Exceptions.BadRequestException;
import com.esprit.demo.Models.User;
import com.esprit.demo.Repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    /**
     * POST /api/auth/login
     * Body: { "username": "...", "password": "..." }
     * username field accepts either username or email.
     * Returns user info (id, username, email, role) on success.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.getUsername().trim();
        String password = request.getPassword();

        // Try to find by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }

        User user = userOpt.orElseThrow(() ->
                new BadRequestException("Identifiant ou mot de passe incorrect"));

        // Plain-text password check (no encoding — as the project does not use Spring Security yet)
        if (!password.equals(user.getPassword())) {
            throw new BadRequestException("Identifiant ou mot de passe incorrect");
        }

        AuthResponse response = AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .xp(user.getXp())
                .level(user.getLevel())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Stateless — just returns success. The client clears its local storage.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
