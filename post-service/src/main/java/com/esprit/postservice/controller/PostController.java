package com.esprit.postservice.controller;

import com.esprit.postservice.dto.PostRequest;
import com.esprit.postservice.dto.PostResponse;
import com.esprit.postservice.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAll() {
        return ResponseEntity.ok(postService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getById(id));
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<PostResponse>> getByThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(postService.getByThread(threadId));
    }

    @GetMapping("/thread/{threadId}/count")
    public ResponseEntity<Long> countByThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(postService.countByThread(threadId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<PostResponse>> getByAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(postService.getByAuthor(authorId));
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(@RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> update(
            @PathVariable Long id,
            @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.update(id, request));
    }

    @PatchMapping("/{id}/best-answer")
    public ResponseEntity<PostResponse> markBestAnswer(
            @PathVariable Long id,
            @RequestParam Long userId) {
        return ResponseEntity.ok(postService.markBestAnswer(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam Long userId) {
        postService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
