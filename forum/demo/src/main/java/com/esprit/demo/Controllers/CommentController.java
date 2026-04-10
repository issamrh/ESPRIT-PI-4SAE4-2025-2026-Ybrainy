package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.CommentRequest;
import com.esprit.demo.Dto.CommentResponse;
import com.esprit.demo.Services.implm.CommentServiceImplm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {


    private final CommentServiceImplm commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getAll() {
        return ResponseEntity.ok(commentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(commentService.getById(id));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponse>> getByPost(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok(commentService.getByPost(postId));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countByPost(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok(commentService.countByPost(postId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<CommentResponse>> getByAuthor(@PathVariable("authorId") Long authorId) {
        return ResponseEntity.ok(commentService.getByAuthor(authorId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> update(@PathVariable("id") Long id,
                                                  @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id,
                                       @RequestParam("userId") Long userId) {
        commentService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }


}
