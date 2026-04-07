package tn.esprit.eventservice.dto;

/**
 * Lightweight user representation received from user-service.
 */
public record UserDto(
        long idUser,
        String nom,
        String prenom,
        String email,
        String role
) {}
