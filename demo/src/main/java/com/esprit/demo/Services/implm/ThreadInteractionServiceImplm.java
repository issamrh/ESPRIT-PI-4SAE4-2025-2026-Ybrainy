package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.ThreadInteractionStatus;
import com.esprit.demo.Dto.ThreadResponse;
import com.esprit.demo.Exceptions.ResourceNotFoundException;
import com.esprit.demo.Mappers.ThreadMapper;
import com.esprit.demo.Models.ReactionType;
import com.esprit.demo.Models.Thread;
import com.esprit.demo.Models.ThreadReaction;
import com.esprit.demo.Models.ThreadVote;
import com.esprit.demo.Models.ThreadWishlist;
import com.esprit.demo.Models.User;
import com.esprit.demo.Models.VoteType;
import com.esprit.demo.Repositories.ThreadReactionRepository;
import com.esprit.demo.Repositories.ThreadRepository;
import com.esprit.demo.Repositories.ThreadVoteRepository;
import com.esprit.demo.Repositories.ThreadWishlistRepository;
import com.esprit.demo.Repositories.UserRepository;
import com.esprit.demo.Services.ThreadInteractionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThreadInteractionServiceImplm implements ThreadInteractionService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final ThreadVoteRepository voteRepository;
    private final ThreadReactionRepository reactionRepository;
    private final ThreadWishlistRepository wishlistRepository;
    private final ThreadMapper threadMapper;
    private final XpServiceImplm xpService;
    private final NotificationServiceImplm notificationService;

    @Override
    @Transactional
    public ThreadInteractionStatus vote(Long threadId, Long userId, VoteType voteType) {
        Thread thread = findThreadOrThrow(threadId);
        User user = findUserOrThrow(userId);
        if (thread.getAuthor().getId().equals(userId)) {
            throw new com.esprit.demo.Exceptions.BadRequestException("Vous ne pouvez pas voter sur votre propre thread.");
        }

        Optional<ThreadVote> existing = voteRepository.findByUserIdAndThreadId(userId, threadId);

        boolean isNewVote = false;
        if (existing.isPresent()) {
            ThreadVote vote = existing.get();
            if (vote.getVoteType() == voteType) {
                // Same type → toggle off (remove vote)
                voteRepository.delete(vote);
            } else {
                // Different type → switch vote
                vote.setVoteType(voteType);
                voteRepository.save(vote);
                if (voteType == VoteType.UPVOTE) {
                    xpService.awardUpvoteReceived(thread.getAuthor().getId(), threadId);
                }
                isNewVote = true;
            }
        } else {
            // New vote
            ThreadVote newVote = ThreadVote.builder()
                    .user(user)
                    .thread(thread)
                    .voteType(voteType)
                    .build();
            voteRepository.save(newVote);
            if (voteType == VoteType.UPVOTE) {
                xpService.awardUpvoteReceived(thread.getAuthor().getId(), threadId);
            }
            isNewVote = true;
        }

        if (isNewVote && !thread.getAuthor().getId().equals(userId)) {
            String label = voteType == VoteType.UPVOTE ? "upvoté" : "downvoté";
            notificationService.create(
                thread.getAuthor().getId(),
                "VOTED",
                user.getUsername() + " a " + label + " votre thread \"" + thread.getTitle() + "\"",
                threadId
            );
        }

        return buildStatus(threadId, userId);
    }

    @Override
    @Transactional
    public ThreadInteractionStatus react(Long threadId, Long userId, ReactionType reactionType) {
        Thread thread = findThreadOrThrow(threadId);
        User user = findUserOrThrow(userId);
        if (thread.getAuthor().getId().equals(userId)) {
            throw new com.esprit.demo.Exceptions.BadRequestException("Vous ne pouvez pas réagir à votre propre thread.");
        }

        Optional<ThreadReaction> existing = reactionRepository.findByUserIdAndThreadId(userId, threadId);

        boolean isNewReaction = false;
        if (existing.isPresent()) {
            ThreadReaction reaction = existing.get();
            if (reaction.getReactionType() == reactionType) {
                // Same → toggle off
                reactionRepository.delete(reaction);
            } else {
                // Different → switch
                reaction.setReactionType(reactionType);
                reactionRepository.save(reaction);
                isNewReaction = true;
            }
        } else {
            ThreadReaction newReaction = ThreadReaction.builder()
                    .user(user)
                    .thread(thread)
                    .reactionType(reactionType)
                    .build();
            reactionRepository.save(newReaction);
            isNewReaction = true;
        }

        if (isNewReaction && !thread.getAuthor().getId().equals(userId)) {
            String label = reactionType == ReactionType.LIKE ? "aimé" : "disliké";
            notificationService.create(
                thread.getAuthor().getId(),
                "REACTED",
                user.getUsername() + " a " + label + " votre thread \"" + thread.getTitle() + "\"",
                threadId
            );
        }

        return buildStatus(threadId, userId);
    }

    @Override
    @Transactional
    public ThreadInteractionStatus toggleWishlist(Long threadId, Long userId) {
        Thread thread = findThreadOrThrow(threadId);
        User user = findUserOrThrow(userId);

        Optional<ThreadWishlist> existing = wishlistRepository.findByUserIdAndThreadId(userId, threadId);

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
        } else {
            ThreadWishlist item = ThreadWishlist.builder()
                    .user(user)
                    .thread(thread)
                    .build();
            wishlistRepository.save(item);
        }

        return buildStatus(threadId, userId);
    }

    @Override
    public ThreadInteractionStatus getStatus(Long threadId, Long userId) {
        findThreadOrThrow(threadId);
        findUserOrThrow(userId);
        return buildStatus(threadId, userId);
    }

    @Override
    public List<ThreadResponse> getWishlist(Long userId) {
        findUserOrThrow(userId);
        return wishlistRepository.findByUserId(userId).stream()
                .map(item -> {
                    Thread thread = item.getThread();
                    long upvotes = voteRepository.countByThreadIdAndVoteType(thread.getId(), VoteType.UPVOTE);
                    long downvotes = voteRepository.countByThreadIdAndVoteType(thread.getId(), VoteType.DOWNVOTE);
                    long likes = reactionRepository.countByThreadIdAndReactionType(thread.getId(), ReactionType.LIKE);
                    long dislikes = reactionRepository.countByThreadIdAndReactionType(thread.getId(), ReactionType.DISLIKE);
                    return threadMapper.toResponse(thread, upvotes, downvotes, likes, dislikes);
                })
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────

    private ThreadInteractionStatus buildStatus(Long threadId, Long userId) {
        long upvotes = voteRepository.countByThreadIdAndVoteType(threadId, VoteType.UPVOTE);
        long downvotes = voteRepository.countByThreadIdAndVoteType(threadId, VoteType.DOWNVOTE);
        long likes = reactionRepository.countByThreadIdAndReactionType(threadId, ReactionType.LIKE);
        long dislikes = reactionRepository.countByThreadIdAndReactionType(threadId, ReactionType.DISLIKE);

        VoteType userVote = voteRepository.findByUserIdAndThreadId(userId, threadId)
                .map(ThreadVote::getVoteType).orElse(null);

        ReactionType userReaction = reactionRepository.findByUserIdAndThreadId(userId, threadId)
                .map(ThreadReaction::getReactionType).orElse(null);

        boolean wishlisted = wishlistRepository.existsByUserIdAndThreadId(userId, threadId);

        return ThreadInteractionStatus.builder()
                .userVote(userVote)
                .userReaction(userReaction)
                .wishlisted(wishlisted)
                .upvoteCount(upvotes)
                .downvoteCount(downvotes)
                .voteScore(upvotes - downvotes)
                .likeCount(likes)
                .dislikeCount(dislikes)
                .build();
    }

    private Thread findThreadOrThrow(Long threadId) {
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread introuvable avec l'id : " + threadId));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + userId));
    }
}
