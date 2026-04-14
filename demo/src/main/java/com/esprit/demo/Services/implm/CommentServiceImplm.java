package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.CommentRequest;
import com.esprit.demo.Dto.CommentResponse;
import com.esprit.demo.Exceptions.ResourceNotFoundException;
import com.esprit.demo.Mappers.CommentMapper;
import com.esprit.demo.Models.Comment;
import com.esprit.demo.Models.Post;
import com.esprit.demo.Models.Status;
import com.esprit.demo.Models.User;
import com.esprit.demo.Repositories.CommentRepository;
import com.esprit.demo.Repositories.PostRepository;
import com.esprit.demo.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import com.esprit.demo.Exceptions.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImplm {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final XpServiceImplm xpService;
    private final NotificationServiceImplm notificationService;

    public CommentResponse create(CommentRequest request) throws BadRequestException {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post introuvable avec l'id : " + request.getPostId()));

        if (post.getThread().getStatus() == Status.LOCKED)
            throw new BadRequestException("Ce thread est verrouillé. Impossible d'ajouter un commentaire");
        if (post.getThread().getStatus() == Status.CLOSED)
            throw new BadRequestException("Ce thread est fermé. Impossible d'ajouter un commentaire");
        if (post.getThread().getAuthor().getId().equals(request.getAuthorId()))
            throw new BadRequestException("Vous ne pouvez pas commenter dans votre propre thread.");

        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + request.getAuthorId()));

        Comment comment = Comment.builder()
                .body(request.getBody())
                .post(post)
                .author(author)
                .build();

        Comment saved = commentRepository.save(comment);
        xpService.awardCommentCreated(author.getId(), saved.getId());

        Long threadOwnerId = post.getThread().getAuthor().getId();
        if (!threadOwnerId.equals(author.getId())) {
            notificationService.create(
                threadOwnerId,
                "COMMENT_CREATED",
                author.getUsername() + " a commenté dans votre thread \"" + post.getThread().getTitle() + "\"",
                post.getThread().getId()
            );
        }

        return commentMapper.toResponse(saved);
    }

    public List<CommentResponse> getAll() {
        return commentRepository.findAll()
                .stream().map(commentMapper::toResponse).collect(Collectors.toList());
    }

    public CommentResponse getById(Long id) {
        return commentMapper.toResponse(findOrThrow(id));
    }

    public List<CommentResponse> getByPost(Long postId) {
        if (!postRepository.existsById(postId))
            throw new ResourceNotFoundException("Post introuvable avec l'id : " + postId);
        return commentRepository.findByPostId(postId)
                .stream().map(commentMapper::toResponse).collect(Collectors.toList());
    }

    public List<CommentResponse> getByAuthor(Long authorId) {
        return commentRepository.findByAuthorId(authorId)
                .stream().map(commentMapper::toResponse).collect(Collectors.toList());
    }

    public long countByPost(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public CommentResponse update(Long id, CommentRequest request)  {
        Comment comment = findOrThrow(id);
        if (comment.getPost().getThread().getStatus() == Status.LOCKED)
            throw new BadRequestException("Ce thread est verrouillé. Impossible de modifier le commentaire");
        comment.setBody(request.getBody());
        return commentMapper.toResponse(commentRepository.save(comment));
    }

    public void delete(Long id, Long userId) {
        Comment comment = findOrThrow(id);
        if (!comment.getAuthor().getId().equals(userId))
            throw new BadRequestException("Vous ne pouvez pas supprimer un commentaire qui ne vous appartient pas.");
        commentRepository.deleteById(id);
    }

    private Comment findOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire introuvable avec l'id : " + id));
    }
}
