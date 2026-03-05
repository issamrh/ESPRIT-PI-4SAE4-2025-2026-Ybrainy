package com.esprit.demo.Services.implm;

import com.esprit.demo.Services.AiGenerationService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Calls the Groq Chat Completions API (OpenAI-compatible, free).
 * Endpoint: POST https://api.groq.com/openai/v1/chat/completions
 * Get a free API key at: https://console.groq.com
 */
@Service
public class AiGenerationServiceImplm implements AiGenerationService {

    // ── Prompt: generate a clear question for the thread body ──────────────
    private static final String THREAD_QUESTION_PROMPT =
            "You are an expert forum question writer. Given a thread title, write a clear, " +
            "detailed, and well-structured forum question. Your question should: provide context " +
            "about the problem or topic, explain what the person has already tried or what they know, " +
            "and be specific about what kind of help or information they need. " +
            "Use a natural, conversational tone. " +
            "Structure your response in 2-3 paragraphs. Do not include a subject line — " +
            "write only the question body. Do not use any markdown formatting like ** or ##.";

    // ── Prompt: generate a helpful answer for a post ───────────────────────
    private static final String POST_ANSWER_PROMPT =
            "You are an expert forum responder. Given a forum thread title and its question body, " +
            "write a helpful, detailed, and accurate answer. Your response should directly address " +
            "the question, provide clear explanations and practical examples where relevant, " +
            "and be structured logically. " +
            "Structure your response in 2-4 paragraphs. " +
            "Do not use any markdown formatting like ** or ##.";

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${ai.api-base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiBaseUrl;

    private final RestClient restClient;

    public AiGenerationServiceImplm() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String generateThreadBody(String title) {
        return callGroq(
                THREAD_QUESTION_PROMPT,
                "Write a forum question body for this thread title: \"" + title + "\""
        );
    }

    @Override
    public String generatePostBody(String threadTitle, String threadBody) {
        String userMessage = "Thread title: \"" + threadTitle + "\"\n\n" +
                "Thread question:\n" + threadBody + "\n\n" +
                "Write a helpful answer to this forum question.";
        return callGroq(POST_ANSWER_PROMPT, userMessage);
    }

    // ── Shared Groq call ───────────────────────────────────────────────────

    private String callGroq(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 600,
                "temperature", 0.7
        );

        ChatResponse response = restClient.post()
                .uri(apiBaseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()) {
            throw new RuntimeException("No response from Groq API");
        }

        String text = response.getChoices().get(0).getMessage().getContent();
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Groq returned empty content");
        }
        return text.trim();
    }

    // ── Internal DTOs matching OpenAI-compatible chat completions response ──

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatResponse {
        private List<Choice> choices;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
    }
}
