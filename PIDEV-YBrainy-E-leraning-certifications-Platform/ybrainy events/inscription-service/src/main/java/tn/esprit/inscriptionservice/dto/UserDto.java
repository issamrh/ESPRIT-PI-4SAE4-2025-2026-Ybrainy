package tn.esprit.inscriptionservice.dto;

public record UserDto(
        long idUser,
        String nom,
        String prenom,
        String email,
        String role
) {
}
