package com.esprit.commentservice.service;

import com.esprit.commentservice.dto.CommentRequest;
import com.esprit.commentservice.dto.CommentResponse;
import com.esprit.commentservice.exception.BadRequestException;
import com.esprit.commentservice.exception.ResourceNotFoundException;
import com.esprit.commentservice.feign.UserFeignClient;
import com.esprit.commentservice.messaging.ForumEventPublisher;
import com.esprit.commentservice.model.Comment;
import com.esprit.commentservice.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommentService.
 * No Spring context — all dependencies are mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private ForumEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    private Comment sampleComment;

    @BeforeEach
    void setUp() {
        sampleComment = Comment.builder()
                .id(1L)
                .content("Great post!")
                .authorId(10L)
                .postId(5L)
                .threadId(3L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — returns CommentResponse when comment exists")
    void getById_found() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));
        when(userFeignClient.getUser(10L)).thenReturn(null); // author fetch is optional

        CommentResponse response = commentService.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBody()).isEqualTo("Great post!");
        assertThat(response.getAuthorId()).isEqualTo(10L);
        assertThat(response.getPostId()).isEqualTo(5L);

        verify(commentRepository).findById(1L);
    }

    @Test
    @DisplayName("getById — throws ResourceNotFoundException when comment does not exist")
    void getById_notFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(commentRepository).findById(99L);
        verifyNoInteractions(eventPublisher);
    }

    // ── getByPost ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByPost — returns list of comments ordered by creation date")
    void getByPost_returnsList() {
        Comment second = Comment.builder()
                .id(2L).content("Me too!").authorId(20L)
                .postId(5L).threadId(3L).createdAt(LocalDateTime.now()).build();

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(5L))
                .thenReturn(List.of(sampleComment, second));

        List<CommentResponse> result = commentService.getByPost(5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getByPost — returns empty list when no comments exist for a post")
    void getByPost_emptyList() {
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(999L))
                .thenReturn(List.of());

        List<CommentResponse> result = commentService.getByPost(999L);

        assertThat(result).isEmpty();
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — saves comment and publishes RabbitMQ event")
    void create_savesAndPublishesEvent() {
        CommentRequest request = new CommentRequest();
        request.setBody("Nice answer!");
        request.setAuthorId(10L);
        request.setPostId(5L);
        request.setThreadId(3L);

        Comment saved = Comment.builder()
                .id(1L).content("Nice answer!")
                .authorId(10L).postId(5L).threadId(3L)
                .createdAt(LocalDateTime.now()).build();

        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentResponse response = commentService.create(request);

        // Assert the returned response maps all fields correctly
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBody()).isEqualTo("Nice answer!");
        assertThat(response.getAuthorId()).isEqualTo(10L);

        // Verify repository save was called once
        verify(commentRepository, times(1)).save(any(Comment.class));

        // Verify the RabbitMQ event was published
        verify(eventPublisher, times(1))
                .publishCommentCreated(1L, 10L, 5L, 3L);
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — updates content when comment exists")
    void update_success() {
        CommentRequest request = new CommentRequest();
        request.setBody("Updated body");

        Comment updated = Comment.builder()
                .id(1L).content("Updated body")
                .authorId(10L).postId(5L).threadId(3L)
                .createdAt(LocalDateTime.now()).build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));
        when(commentRepository.save(sampleComment)).thenReturn(updated);

        CommentResponse response = commentService.update(1L, request);

        assertThat(response.getBody()).isEqualTo("Updated body");
        verify(commentRepository).save(sampleComment);
    }

    @Test
    @DisplayName("update — throws ResourceNotFoundException when comment does not exist")
    void update_notFound() {
        when(commentRepository.findById(55L)).thenReturn(Optional.empty());

        CommentRequest request = new CommentRequest();
        request.setBody("something");

        assertThatThrownBy(() -> commentService.update(55L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("55");

        verify(commentRepository, never()).save(any());
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — removes comment when caller is the owner")
    void delete_ownerCanDelete() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));

        // authorId = 10L, userId = 10L → same person → should succeed
        commentService.delete(1L, 10L);

        verify(commentRepository).delete(sampleComment);
    }

    @Test
    @DisplayName("delete — throws BadRequestException when caller is NOT the owner")
    void delete_nonOwnerCannotDelete() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));

        // authorId = 10L, but userId = 99L → different person → must be rejected
        assertThatThrownBy(() -> commentService.delete(1L, 99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("owner");

        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete — throws ResourceNotFoundException when comment does not exist")
    void delete_commentNotFound() {
        when(commentRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(77L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("77");
    }

    // ── countByPost ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("countByPost — returns the count from repository")
    void countByPost_returnsCount() {
        when(commentRepository.countByPostId(5L)).thenReturn(3L);

        long count = commentService.countByPost(5L);

        assertThat(count).isEqualTo(3L);
        verify(commentRepository).countByPostId(5L);
    }
}
