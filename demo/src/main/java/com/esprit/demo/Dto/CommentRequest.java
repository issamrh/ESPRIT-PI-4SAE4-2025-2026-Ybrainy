package com.esprit.demo.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Le commentaire est obligatoire")
    @Size(min = 2, max = 1000, message = "Le commentaire doit avoir entre 2 et 1000 caractères")
    private String body;

    @NotNull(message = "Le post est obligatoire")
    private Long postId;

    @NotNull(message = "L'auteur est obligatoire")
    private Long authorId;

}
