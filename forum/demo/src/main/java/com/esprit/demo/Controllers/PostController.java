package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.PostRequest;
import com.esprit.demo.Dto.PostResponse;
import com.esprit.demo.Services.implm.PostServiceImplm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostServiceImplm postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @RequestParam("body") String body,
            @RequestParam("threadId") Long threadId,
            @RequestParam("authorId") Long authorId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file",  required = false) MultipartFile file) {

        PostRequest request = new PostRequest();
        request.setBody(body);
        request.setThreadId(threadId);
        request.setAuthorId(authorId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.create(request, image, file));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAll() {
        return ResponseEntity.ok(postService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(postService.getById(id));
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<PostResponse>> getByThread(@PathVariable("threadId") Long threadId) {
        return ResponseEntity.ok(postService.getByThread(threadId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<PostResponse>> getByAuthor(@PathVariable("authorId") Long authorId) {
        return ResponseEntity.ok(postService.getByAuthor(authorId));
    }

    @GetMapping("/thread/{threadId}/count")
    public ResponseEntity<Long> countByThread(@PathVariable("threadId") Long threadId) {
        return ResponseEntity.ok(postService.countByThread(threadId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> update(
            @PathVariable("id") Long id,
            @RequestParam("body") String body,
            @RequestParam("threadId") Long threadId,
            @RequestParam("authorId") Long authorId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file",  required = false) MultipartFile file) {

        PostRequest request = new PostRequest();
        request.setBody(body);
        request.setThreadId(threadId);
        request.setAuthorId(authorId);

        return ResponseEntity.ok(postService.update(id, request, image, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id,
                                       @RequestParam("userId") Long userId) {
        postService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
