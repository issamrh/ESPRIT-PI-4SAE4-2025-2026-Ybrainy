package com.ybrainy.backend.controller;

import com.ybrainy.backend.dto.pack.PackResponseDTO;
import com.ybrainy.backend.service.PackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packs")
@RequiredArgsConstructor
public class PublicPackController {

    private final PackService packService;

    @GetMapping("/active")
    public ResponseEntity<List<PackResponseDTO>> getActivePacks() {
        return ResponseEntity.ok(packService.getActivePacks());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<PackResponseDTO>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(packService.getActivePacksByCategory(categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(packService.getActivePackById(id));
    }
}

