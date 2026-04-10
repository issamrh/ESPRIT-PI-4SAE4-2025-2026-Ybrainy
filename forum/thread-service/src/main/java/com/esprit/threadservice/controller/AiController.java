package com.esprit.threadservice.controller;

import com.esprit.threadservice.dto.AiChatRequest;
import com.esprit.threadservice.dto.AiChatResponse;
import com.esprit.threadservice.dto.AiGeneratePostRequest;
import com.esprit.threadservice.dto.AiGenerateRequest;
import com.esprit.threadservice.dto.AiGenerateResponse;
import com.esprit.threadservice.service.AiGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiGenerationService aiGenerationService;

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

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        log.info("yForumy chat: {}", request.getMessage());
        try {
            return ResponseEntity.ok(aiGenerationService.chat(request));
        } catch (Exception e) {
            log.error("yForumy chat failed: {}", e.getMessage());
            return ResponseEntity.ok(AiChatResponse.builder()
                    .reply("Sorry, I encountered an error. Please try again.")
                    .relatedThreads(java.util.Collections.emptyList())
                    .build());
        }
    }
}
