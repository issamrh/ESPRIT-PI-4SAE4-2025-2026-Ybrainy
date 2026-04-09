package com.esprit.demo.Services;

import com.esprit.demo.Dto.AiChatRequest;
import com.esprit.demo.Dto.AiChatResponse;

public interface AiGenerationService {
    /** Generate a well-structured question body for a new thread, based on its title. */
    String generateThreadBody(String title);

    /** Generate a helpful answer/response for a post, based on the thread title and body. */
    String generatePostBody(String threadTitle, String threadBody);

    /** Chat with yForumy AI assistant, with forum DB context. */
    AiChatResponse chat(AiChatRequest request);
}
