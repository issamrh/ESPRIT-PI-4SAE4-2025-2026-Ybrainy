package com.esprit.threadservice.controller;

import com.esprit.threadservice.dto.ThreadRequest;
import com.esprit.threadservice.dto.ThreadResponse;
import com.esprit.threadservice.model.ThreadStatus;
import com.esprit.threadservice.service.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;

    @GetMapping
    public ResponseEntity<List<ThreadResponse>> getAll(
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(threadService.getAll(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThreadResponse> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(threadService.getById(id, userId));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ThreadResponse>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(threadService.getByCategory(categoryId, userId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<ThreadResponse>> getByAuthor(
            @PathVariable Long authorId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(threadService.getByAuthor(authorId, userId));
    }

    @PostMapping
    public ResponseEntity<ThreadResponse> create(@RequestBody ThreadRequest request) {
        return ResponseEntity.ok(threadService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThreadResponse> update(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody ThreadRequest request) {
        return ResponseEntity.ok(threadService.update(id, userId, request));
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<ThreadResponse> lock(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(threadService.updateStatus(id, userId, ThreadStatus.LOCKED));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ThreadResponse> unlock(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(threadService.updateStatus(id, userId, ThreadStatus.OPEN));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ThreadResponse> close(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(threadService.updateStatus(id, userId, ThreadStatus.CLOSED));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<ThreadResponse> reopen(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(threadService.updateStatus(id, userId, ThreadStatus.OPEN));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id, @RequestParam Long userId) {
        threadService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
