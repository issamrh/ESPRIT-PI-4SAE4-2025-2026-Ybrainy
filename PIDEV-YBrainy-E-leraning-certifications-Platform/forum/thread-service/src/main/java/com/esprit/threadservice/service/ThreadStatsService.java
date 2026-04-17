package com.esprit.threadservice.service;

import com.esprit.threadservice.dto.ThreadUserStatsDto;
import com.esprit.threadservice.model.ForumThread;
import com.esprit.threadservice.model.ReactionType;
import com.esprit.threadservice.model.VoteType;
import com.esprit.threadservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThreadStatsService {

    private final ThreadRepository threadRepository;
    private final ThreadVoteRepository voteRepository;
    private final ThreadReactionRepository reactionRepository;
    private final ThreadWishlistRepository wishlistRepository;

    public ThreadUserStatsDto getStatsForUser(Long userId) {
        List<ForumThread> userThreads = threadRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
        long threadCount = userThreads.size();

        // Aggregate votes/reactions on all the user's threads
        long upvotes   = 0, downvotes = 0, likes = 0, dislikes = 0, saves = 0;
        Long bestId    = null;
        String bestTitle = null;
        long bestUpvotes = 0;
        long bestTotalReactions = 0;

        for (ForumThread t : userThreads) {
            long u  = voteRepository.countByThreadIdAndVoteType(t.getId(), VoteType.UPVOTE);
            long d  = voteRepository.countByThreadIdAndVoteType(t.getId(), VoteType.DOWNVOTE);
            long l  = reactionRepository.countByThreadIdAndReactionType(t.getId(), ReactionType.LIKE);
            long di = reactionRepository.countByThreadIdAndReactionType(t.getId(), ReactionType.DISLIKE);
            long wl = wishlistRepository.findByUserId(userId).stream()
                    .filter(w -> w.getThread().getId().equals(t.getId())).count();

            upvotes   += u;
            downvotes += d;
            likes     += l;
            dislikes  += di;
            saves     += wl;

            if (u > bestUpvotes) {
                bestUpvotes         = u;
                bestTotalReactions  = u + d + l + di;
                bestId              = t.getId();
                bestTitle           = t.getTitle();
            }
        }

        // Community totals
        long commUpvotes   = voteRepository.count();   // approximate — just total votes
        long commDownvotes = 0;
        long commLikes     = 0;
        long commDislikes  = 0;

        // More accurate per-type community counts
        for (VoteType vt : VoteType.values()) {
            long c = voteRepository.findAll().stream()
                    .filter(v -> v.getVoteType() == vt).count();
            if (vt == VoteType.UPVOTE)   commUpvotes   = c;
            if (vt == VoteType.DOWNVOTE) commDownvotes = c;
        }
        for (ReactionType rt : ReactionType.values()) {
            long c = reactionRepository.findAll().stream()
                    .filter(r -> r.getReactionType() == rt).count();
            if (rt == ReactionType.LIKE)    commLikes    = c;
            if (rt == ReactionType.DISLIKE) commDislikes = c;
        }

        // Weekly activity
        List<ThreadUserStatsDto.WeekBucket> weekly = buildWeeklyActivity(userThreads);

        return ThreadUserStatsDto.builder()
                .threadCount(threadCount)
                .upvotesReceived(upvotes)
                .downvotesReceived(downvotes)
                .likesReceived(likes)
                .dislikesReceived(dislikes)
                .savesReceived(saves)
                .communityTotalThreads(threadRepository.count())
                .communityTotalUpvotes(commUpvotes)
                .communityTotalDownvotes(commDownvotes)
                .communityTotalLikes(commLikes)
                .communityTotalDislikes(commDislikes)
                .bestThreadId(bestId)
                .bestThreadTitle(bestTitle)
                .bestThreadUpvotes(bestUpvotes)
                .bestThreadTotalReactions(bestTotalReactions)
                .weeklyActivity(weekly)
                .build();
    }

    private List<ThreadUserStatsDto.WeekBucket> buildWeeklyActivity(List<ForumThread> threads) {
        LocalDateTime since = LocalDateTime.now().minusWeeks(12);
        LinkedHashMap<String, long[]> weekMap = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime w = LocalDateTime.now().minusWeeks(i);
            weekMap.put(weekKey(w), new long[1]);
        }
        threads.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                .forEach(t -> {
                    long[] v = weekMap.get(weekKey(t.getCreatedAt()));
                    if (v != null) v[0]++;
                });
        return weekMap.entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("-");
                    String label = "Sem " + (parts.length > 1 ? parts[1] : e.getKey());
                    return ThreadUserStatsDto.WeekBucket.builder()
                            .weekLabel(label).threadsCount(e.getValue()[0]).build();
                })
                .collect(Collectors.toList());
    }

    private String weekKey(LocalDateTime dt) {
        int year = dt.getYear();
        int week = dt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return year + "-" + String.format("%02d", week);
    }
}
