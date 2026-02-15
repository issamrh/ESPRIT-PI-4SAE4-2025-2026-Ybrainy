package com.ybrainy.backend.controller;

import com.ybrainy.backend.dto.packcategory.CreatePackCategoryDTO;
import com.ybrainy.backend.dto.packcategory.PackCategoryResponseDTO;
import com.ybrainy.backend.dto.packcategory.UpdatePackCategoryDTO;
import com.ybrainy.backend.service.PackCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdminPackCategoryController {

    private final PackCategoryService categoryService;

    @PostMapping
    public ResponseEntity<PackCategoryResponseDTO> create(@Valid @RequestBody CreatePackCategoryDTO dto) {
        return new ResponseEntity<>(categoryService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PackCategoryResponseDTO> update(@PathVariable Long id, @Valid @RequestBody UpdatePackCategoryDTO dto) {
        return ResponseEntity.ok(categoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PackCategoryResponseDTO> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.toggleStatus(id));
    }

    @GetMapping
    public ResponseEntity<List<PackCategoryResponseDTO>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackCategoryResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }
}

