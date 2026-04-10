package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.ThreadRequest;
import com.esprit.demo.Dto.ThreadResponse;
import com.esprit.demo.Exceptions.ResourceNotFoundException;
import com.esprit.demo.Exceptions.BadRequestException;
import com.esprit.demo.Mappers.ThreadMapper;
import com.esprit.demo.Models.Category;
import com.esprit.demo.Models.Status;
import com.esprit.demo.Models.User;
import com.esprit.demo.Models.VoteType;
import com.esprit.demo.Models.ReactionType;
import com.esprit.demo.Repositories.CategoryRepository;
import com.esprit.demo.Repositories.CommentRepository;
import com.esprit.demo.Repositories.PostRepository;
import com.esprit.demo.Repositories.ThreadRepository;
import com.esprit.demo.Repositories.ThreadVoteRepository;
import com.esprit.demo.Repositories.ThreadReactionRepository;
import com.esprit.demo.Repositories.ThreadWishlistRepository;
import com.esprit.demo.Repositories.UserRepository;
import com.esprit.demo.Services.FileStorageService;
import com.esprit.demo.Services.ThreadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.esprit.demo.Models.Thread;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThreadServiceImplm  {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ThreadMapper threadMapper;
    private final XpServiceImplm xpService;
    private final FileStorageService fileStorageService;
    private final ThreadVoteRepository voteRepository;
    private final ThreadReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ThreadWishlistRepository wishlistRepository;




    public ThreadResponse create(ThreadRequest request, MultipartFile image, MultipartFile file) {
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + request.getAuthorId()));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id : " + request.getCategoryId()));
        }
        String imageUrl = fileStorageService.store(image, "threads");

        String fileUrl = null;
        String fileType = null;
        if (file != null && !file.isEmpty()) {
            FileStorageService.StoredFile stored = fileStorageService.storeFile(file, "threads");
            fileUrl  = stored.url();
            fileType = stored.mimeType();
        }

        Thread thread = Thread.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .imageUrl(imageUrl)
                .fileUrl(fileUrl)
                .fileType(fileType)
                .author(author)
                .category(category)
                .status(Status.OPEN)
                .build();

        Thread saved = threadRepository.save(thread);
        xpService.awardThreadCreated(author.getId(), saved.getId());
        return threadMapper.toResponse(saved);
    }

    public List<ThreadResponse> getAll() {
        return threadRepository.findAll()
                .stream()
                .map(this::enriched)
                .sorted(Comparator.comparingLong(ThreadResponse::getVoteScore).reversed())
                .collect(Collectors.toList());
    }

    public ThreadResponse getById(Long id) {
        return enriched(findOrThrow(id));
    }

    public List<ThreadResponse> getByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId))
            throw new ResourceNotFoundException("Catégorie introuvable avec l'id : " + categoryId);
        return threadRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::enriched)
                .sorted(Comparator.comparingLong(ThreadResponse::getVoteScore).reversed())
                .collect(Collectors.toList());
    }

    public List<ThreadResponse> getByAuthor(Long authorId) {
        return threadRepository.findByAuthorId(authorId)
                .stream()
                .map(this::enriched)
                .collect(Collectors.toList());
    }

    private ThreadResponse enriched(Thread thread) {
        long upvotes   = voteRepository.countByThreadIdAndVoteType(thread.getId(), VoteType.UPVOTE);
        long downvotes = voteRepository.countByThreadIdAndVoteType(thread.getId(), VoteType.DOWNVOTE);
        long likes     = reactionRepository.countByThreadIdAndReactionType(thread.getId(), ReactionType.LIKE);
        long dislikes  = reactionRepository.countByThreadIdAndReactionType(thread.getId(), ReactionType.DISLIKE);
        return threadMapper.toResponse(thread, upvotes, downvotes, likes, dislikes);
    }

    public ThreadResponse update(Long id, ThreadRequest request, MultipartFile image, MultipartFile file) {
        Thread thread = findOrThrow(id);
        if (thread.getStatus() == Status.LOCKED)
            throw new BadRequestException("Impossible de modifier un thread verrouillé");

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id : " + request.getCategoryId()));
            thread.setCategory(category);
        }
        thread.setTitle(request.getTitle());
        thread.setBody(request.getBody());
        if (image != null && !image.isEmpty()) {
            fileStorageService.delete(thread.getImageUrl());
            thread.setImageUrl(fileStorageService.store(image, "threads"));
        }
        if (file != null && !file.isEmpty()) {
            fileStorageService.delete(thread.getFileUrl());
            FileStorageService.StoredFile stored = fileStorageService.storeFile(file, "threads");
            thread.setFileUrl(stored.url());
            thread.setFileType(stored.mimeType());
        }
        return enriched(threadRepository.save(thread));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Thread thread = findOrThrow(id);
        if (!thread.getAuthor().getId().equals(userId))
            throw new BadRequestException("Vous ne pouvez pas supprimer un thread qui ne vous appartient pas.");

        fileStorageService.delete(thread.getImageUrl());
        fileStorageService.delete(thread.getFileUrl());

        // Cascade delete in correct order to avoid FK violations
        commentRepository.deleteByPostThreadId(id);
        postRepository.deleteByThreadId(id);
        voteRepository.deleteByThreadId(id);
        reactionRepository.deleteByThreadId(id);
        wishlistRepository.deleteByThreadId(id);
        threadRepository.deleteById(id);
    }

    public ThreadResponse lock(Long id, Long userId) throws BadRequestException {
        Thread thread = findOrThrow(id);
        if (!thread.getAuthor().getId().equals(userId))
            throw new BadRequestException("Seul le propriétaire peut verrouiller ce thread.");
        if (thread.getStatus() == Status.LOCKED)
            throw new BadRequestException("Ce thread est déjà verrouillé");
        thread.setStatus(Status.LOCKED);
        return enriched(threadRepository.save(thread));
    }

    public ThreadResponse unlock(Long id, Long userId) {
        Thread thread = findOrThrow(id);
        if (!thread.getAuthor().getId().equals(userId))
            throw new BadRequestException("Seul le propriétaire peut déverrouiller ce thread.");
        if (thread.getStatus() != Status.LOCKED)
            throw new BadRequestException("Ce thread n'est pas verrouillé");
        thread.setStatus(Status.OPEN);
        return enriched(threadRepository.save(thread));
    }

    public ThreadResponse close(Long id, Long userId) {
        Thread thread = findOrThrow(id);
        if (!thread.getAuthor().getId().equals(userId))
            throw new BadRequestException("Seul le propriétaire peut fermer ce thread.");
        if (thread.getStatus() == Status.CLOSED)
            throw new BadRequestException("Ce thread est déjà fermé");
        thread.setStatus(Status.CLOSED);
        return enriched(threadRepository.save(thread));
    }

    public ThreadResponse reopen(Long id, Long userId) {
        Thread thread = findOrThrow(id);
        if (!thread.getAuthor().getId().equals(userId))
            throw new BadRequestException("Seul le propriétaire peut rouvrir ce thread.");
        if (thread.getStatus() == Status.OPEN)
            throw new BadRequestException("Ce thread est déjà ouvert");
        if (thread.getStatus() == Status.LOCKED)
            throw new BadRequestException("Déverrouillez le thread avant de le rouvrir");
        thread.setStatus(Status.OPEN);
        return enriched(threadRepository.save(thread));
    }

    private Thread findOrThrow(Long id) {
        return threadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thread introuvable avec l'id : " + id));
    }
}
