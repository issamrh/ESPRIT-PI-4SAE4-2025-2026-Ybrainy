package com.ybrainy.backend.dto.pack;

public record GeneratePackContentResponseDTO(
        String generatedTitle,
        String generatedDescription,
        String providerMessage
) {
}
