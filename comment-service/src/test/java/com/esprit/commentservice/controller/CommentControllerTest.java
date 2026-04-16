package com.esprit.commentservice.controller;

import com.esprit.commentservice.dto.CommentResponse;
import com.esprit.commentservice.exception.ResourceNotFoundException;
import com.esprit.commentservice.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CommentController (web layer only).
 * The CommentService is mocked — no DB, no Eureka, no RabbitMQ needed.
 */
@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = CommentResponse.builder()
                .id(1L)
                .body("Great post!")
                .authorId(10L)
                .postId(5L)
                .threadId(3L)
                .createdAt(LocalDateTime.of(2026, 4, 16, 10, 0))
                .build();
    }

    // ── GET /api/comments/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/comments/{id} — 200 OK with body when comment exists")
    void getById_returns200() throws Exception {
        when(commentService.getById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/comments/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.body").value("Great post!"))
                .andExpect(jsonPath("$.authorId").value(10))
                .andExpect(jsonPath("$.postId").value(5));

        verify(commentService).getById(1L);
    }

    @Test
    @DisplayName("GET /api/comments/{id} — 404 when comment not found")
    void getById_returns404() throws Exception {
        when(commentService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Comment not found: 99"));

        mockMvc.perform(get("/api/comments/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/comments/post/{postId} ───────────────────────────────────────

    @Test
    @DisplayName("GET /api/comments/post/{postId} — 200 OK with list")
    void getByPost_returns200WithList() throws Exception {
        CommentResponse second = CommentResponse.builder()
                .id(2L).body("Me too!").authorId(20L).postId(5L).threadId(3L)
                .createdAt(LocalDateTime.now()).build();

        when(commentService.getByPost(5L)).thenReturn(List.of(sampleResponse, second));

        mockMvc.perform(get("/api/comments/post/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("GET /api/comments/post/{postId} — 200 OK with empty list")
    void getByPost_returns200EmptyList() throws Exception {
        when(commentService.getByPost(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/comments/post/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /api/comments ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/comments — 200 OK and returns created comment")
    void create_returns200() throws Exception {
        Map<String, Object> body = Map.of(
                "body", "Great post!",
                "authorId", 10,
                "postId", 5,
                "threadId", 3
        );

        when(commentService.create(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.body").value("Great post!"))
                .andExpect(jsonPath("$.authorId").value(10));

        verify(commentService, times(1)).create(any());
    }

    // ── PUT /api/comments/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/comments/{id} — 200 OK with updated body")
    void update_returns200() throws Exception {
        CommentResponse updated = CommentResponse.builder()
                .id(1L).body("Updated content").authorId(10L)
                .postId(5L).threadId(3L).build();

        when(commentService.update(eq(1L), any())).thenReturn(updated);

        Map<String, Object> body = Map.of("body", "Updated content");

        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Updated content"));
    }

    // ── DELETE /api/comments/{id} ──────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/comments/{id}?userId=X — 204 No Content on success")
    void delete_returns204() throws Exception {
        doNothing().when(commentService).delete(1L, 10L);

        mockMvc.perform(delete("/api/comments/1").param("userId", "10"))
                .andExpect(status().isNoContent());

        verify(commentService).delete(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/comments/{id}?userId=X — 400 when userId is not the owner")
    void delete_returns400WhenNotOwner() throws Exception {
        doThrow(new com.esprit.commentservice.exception.BadRequestException("Only the comment owner can delete it"))
                .when(commentService).delete(1L, 99L);

        mockMvc.perform(delete("/api/comments/1").param("userId", "99"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/comments/post/{postId}/count ──────────────────────────────────

    @Test
    @DisplayName("GET /api/comments/post/{postId}/count — 200 OK with count")
    void countByPost_returns200() throws Exception {
        when(commentService.countByPost(5L)).thenReturn(3L);

        mockMvc.perform(get("/api/comments/post/5/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
