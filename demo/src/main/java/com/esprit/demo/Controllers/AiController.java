package com.esprit.demo.Controllers;

import com.esprit.demo.Dto.AiChatRequest;
import com.esprit.demo.Dto.AiChatResponse;
import com.esprit.demo.Dto.AiGeneratePostRequest;
import com.esprit.demo.Dto.AiGenerateRequest;
import com.esprit.demo.Dto.AiGenerateResponse;
import com.esprit.demo.Services.AiGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AiController {

    private final AiGenerationService aiGenerationService;

    /**
     * Generate a well-structured question body for a new thread, based on its title.
     * POST /api/ai/generate-thread-body
     * Body: { "title": "How to learn Spring Boot fast?" }
     */
    @PostMapping("/generate-thread-body")
    public ResponseEntity<AiGenerateResponse> generateThreadBody(
            @Valid @RequestBody AiGenerateRequest request) {
        log.info("AI question generation for title: {}", request.getTitle());
        try {
            String generated = aiGenerationService.generateThreadBody(request.getTitle());
            return ResponseEntity.ok(AiGenerateResponse.builder().body(generated).build());
        } catch (Exception e) {
            log.error("AI thread generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(AiGenerateResponse.builder().body("AI generation failed: " + e.getMessage()).build());
        }
    }

    /**
     * Generate a helpful answer for a post, based on the thread title and body.
     * POST /api/ai/generate-post-body
     * Body: { "threadTitle": "...", "threadBody": "..." }
     */
    @PostMapping("/generate-post-body")
    public ResponseEntity<AiGenerateResponse> generatePostBody(
            @Valid @RequestBody AiGeneratePostRequest request) {
        log.info("AI answer generation for thread: {}", request.getThreadTitle());
        try {
            String generated = aiGenerationService.generatePostBody(
                    request.getThreadTitle(),
                    request.getThreadBody() != null ? request.getThreadBody() : "");
            return ResponseEntity.ok(AiGenerateResponse.builder().body(generated).build());
        } catch (Exception e) {
            log.error("AI post generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(AiGenerateResponse.builder().body("AI generation failed: " + e.getMessage()).build());
        }
    }

    /**
     * yForumy AI assistant chat with forum DB context.
     * POST /api/ai/chat
     * Body: { "message": "...", "history": [{role, content}, ...] }
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        log.info("yForumy chat: {}", request.getMessage());
        try {
            AiChatResponse response = aiGenerationService.chat(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("yForumy chat failed: {}", e.getMessage());
            return ResponseEntity.ok(AiChatResponse.builder()
                    .reply("Sorry, I encountered an error. Please try again.")
                    .relatedThreads(java.util.Collections.emptyList())
                    .build());
        }
    }
}
