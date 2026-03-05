package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.PostRequest;
import com.esprit.demo.Dto.PostResponse;
import com.esprit.demo.Exceptions.ResourceNotFoundException;
import com.esprit.demo.Mappers.PostMapper;
import com.esprit.demo.Models.Post;
import com.esprit.demo.Models.Status;
import com.esprit.demo.Models.Thread;
import com.esprit.demo.Models.User;
import com.esprit.demo.Repositories.CommentRepository;
import com.esprit.demo.Repositories.PostRepository;
import com.esprit.demo.Repositories.ThreadRepository;
import com.esprit.demo.Repositories.UserRepository;
import com.esprit.demo.Services.FileStorageService;
import lombok.RequiredArgsConstructor;
import com.esprit.demo.Exceptions.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PostServiceImplm {

    private final PostRepository postRepository;
    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final XpServiceImplm xpService;
    private final FileStorageService fileStorageService;
    private final CommentRepository commentRepository;
    private final NotificationServiceImplm notificationService;



    public PostResponse create(PostRequest request, MultipartFile image, MultipartFile file) {
        Thread thread = threadRepository.findById(request.getThreadId())
                .orElseThrow(() -> new ResourceNotFoundException("Thread introuvable avec l'id : " + request.getThreadId()));

        if (thread.getStatus() == Status.LOCKED)
            throw new BadRequestException("Ce thread est verrouillé. Impossible d'ajouter un post");
        if (thread.getStatus() == Status.CLOSED)
            throw new BadRequestException("Ce thread est fermé. Impossible d'ajouter un post");
        if (thread.getAuthor().getId().equals(request.getAuthorId()))
            throw new BadRequestException("Vous ne pouvez pas poster dans votre propre thread. Créez un nouveau thread.");

        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + request.getAuthorId()));

        String imageUrl = fileStorageService.store(image, "posts");

        String fileUrl = null;
        String fileType = null;
        if (file != null && !file.isEmpty()) {
            FileStorageService.StoredFile stored = fileStorageService.storeFile(file, "posts");
            fileUrl  = stored.url();
            fileType = stored.mimeType();
        }

        Post post = Post.builder()
                .body(request.getBody())
                .imageUrl(imageUrl)
                .fileUrl(fileUrl)
                .fileType(fileType)
                .thread(thread)
                .author(author)
                .build();

        Post saved = postRepository.save(post);
        xpService.awardPostCreated(author.getId(), saved.getId(), request.getBody());

        Long threadOwnerId = thread.getAuthor().getId();
        if (!threadOwnerId.equals(author.getId())) {
            notificationService.create(
                threadOwnerId,
                "POST_CREATED",
                author.getUsername() + " a répondu à votre thread \"" + thread.getTitle() + "\"",
                thread.getId()
            );
        }

        return postMapper.toResponse(saved);
    }

    public List<PostResponse> getAll() {
        return postRepository.findAll()
                .stream().map(postMapper::toResponse).collect(Collectors.toList());
    }

    public PostResponse getById(Long id) {
        return postMapper.toResponse(findOrThrow(id));
    }

    public List<PostResponse> getByThread(Long threadId) {
        if (!threadRepository.existsById(threadId))
            throw new ResourceNotFoundException("Thread introuvable avec l'id : " + threadId);
        return postRepository.findByThreadId(threadId)
                .stream().map(postMapper::toResponse).collect(Collectors.toList());
    }

    public List<PostResponse> getByAuthor(Long authorId) {
        return postRepository.findByAuthorId(authorId)
                .stream().map(postMapper::toResponse).collect(Collectors.toList());
    }

    public long countByThread(Long threadId) {
        return postRepository.countByThreadId(threadId);
    }

    public PostResponse update(Long id, PostRequest request, MultipartFile image, MultipartFile file) {
        Post post = findOrThrow(id);
        if (post.getThread().getStatus() == Status.LOCKED)
            throw new BadRequestException("Ce thread est verrouillé. Impossible de modifier le post");
        post.setBody(request.getBody());

        if (image != null && !image.isEmpty()) {
            fileStorageService.delete(post.getImageUrl());
            post.setImageUrl(fileStorageService.store(image, "posts"));
        }
        if (file != null && !file.isEmpty()) {
            fileStorageService.delete(post.getFileUrl());
            FileStorageService.StoredFile stored = fileStorageService.storeFile(file, "posts");
            post.setFileUrl(stored.url());
            post.setFileType(stored.mimeType());
        }
        return postMapper.toResponse(postRepository.save(post));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Post post = findOrThrow(id);
        if (!post.getAuthor().getId().equals(userId))
            throw new BadRequestException("Vous ne pouvez pas supprimer un post qui ne vous appartient pas.");

        fileStorageService.delete(post.getImageUrl());
        fileStorageService.delete(post.getFileUrl());
        commentRepository.deleteByPostId(id);
        postRepository.deleteById(id);
    }

    private Post findOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post introuvable avec l'id : " + id));
    }
}
