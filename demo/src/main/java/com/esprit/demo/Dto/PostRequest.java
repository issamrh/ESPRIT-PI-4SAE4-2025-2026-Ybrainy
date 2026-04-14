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
public class PostRequest {

    @NotBlank(message = "Le contenu est obligatoire")
    @Size(min = 2, message = "Le contenu doit avoir au moins 2 caractères")
    private String body;

    @NotNull(message = "Le thread est obligatoire")
    private Long threadId;

    @NotNull(message = "L'auteur est obligatoire")
    private Long authorId;
}
