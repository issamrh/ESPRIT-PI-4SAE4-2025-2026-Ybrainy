package com.esprit.commentservice.controller;

import com.esprit.commentservice.dto.CommentRequest;
import com.esprit.commentservice.dto.CommentResponse;
import com.esprit.commentservice.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getAll() {
        return ResponseEntity.ok(commentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getById(id));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponse>> getByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getByPost(postId));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.countByPost(postId));
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<CommentResponse>> getByThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(commentService.getByThread(threadId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<CommentResponse>> getByAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(commentService.getByAuthor(authorId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long id,
            @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam Long userId) {
        commentService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
