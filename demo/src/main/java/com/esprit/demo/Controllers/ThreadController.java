package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.ThreadRequest;
import com.esprit.demo.Dto.ThreadResponse;
import com.esprit.demo.Services.implm.ThreadServiceImplm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadServiceImplm threadService;

    /**
     * POST /api/threads
     * Content-Type: multipart/form-data
     * Champs : title, body, authorId, categoryId (optionnel), image (optionnel)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThreadResponse> create(
            @RequestParam("title") String title,
            @RequestParam("body") String body,
            @RequestParam("authorId") Long authorId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file",  required = false) MultipartFile file) {

        ThreadRequest request = new ThreadRequest();
        request.setTitle(title);
        request.setBody(body);
        request.setAuthorId(authorId);
        request.setCategoryId(categoryId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(threadService.create(request, image, file));
    }

    @GetMapping
    public ResponseEntity<List<ThreadResponse>> getAll() {
        return ResponseEntity.ok(threadService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThreadResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(threadService.getById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ThreadResponse>> getByCategory(@PathVariable("categoryId") Long categoryId) {
        return ResponseEntity.ok(threadService.getByCategory(categoryId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<ThreadResponse>> getByAuthor(@PathVariable("authorId") Long authorId) {
        return ResponseEntity.ok(threadService.getByAuthor(authorId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThreadResponse> update(
            @PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("body") String body,
            @RequestParam("authorId") Long authorId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file",  required = false) MultipartFile file) {

        ThreadRequest request = new ThreadRequest();
        request.setTitle(title);
        request.setBody(body);
        request.setAuthorId(authorId);
        request.setCategoryId(categoryId);

        return ResponseEntity.ok(threadService.update(id, request, image, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id,
                                       @RequestParam("userId") Long userId) {
        threadService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<ThreadResponse> lock(@PathVariable("id") Long id,
                                               @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(threadService.lock(id, userId));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ThreadResponse> unlock(@PathVariable("id") Long id,
                                                 @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(threadService.unlock(id, userId));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ThreadResponse> close(@PathVariable("id") Long id,
                                                @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(threadService.close(id, userId));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<ThreadResponse> reopen(@PathVariable("id") Long id,
                                                 @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(threadService.reopen(id, userId));
    }
}
