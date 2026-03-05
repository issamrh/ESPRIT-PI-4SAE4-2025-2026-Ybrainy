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
public class ThreadRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 5, max = 200, message = "Le titre doit avoir entre 5 et 200 caractères")
    private String title;

    @NotBlank(message = "Le contenu est obligatoire")
    @Size(min = 10, message = "Le contenu doit avoir au moins 10 caractères")
    private String body;

    @NotNull(message = "L'auteur est obligatoire")
    private Long authorId;

    private Long categoryId;

}
