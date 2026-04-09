package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.*;
import com.esprit.demo.Exceptions.ResourceNotFoundException;
import com.esprit.demo.Models.Comment;
import com.esprit.demo.Models.Post;
import com.esprit.demo.Models.ReactionType;
import com.esprit.demo.Models.User;
import com.esprit.demo.Models.VoteType;
import com.esprit.demo.Repositories.*;
import com.esprit.demo.Services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImplm implements DashboardService {

    private final UserRepository userRepo;
    private final ThreadRepository threadRepo;
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final ThreadVoteRepository voteRepo;
    private final ThreadReactionRepository reactionRepo;
    private final ThreadWishlistRepository wishlistRepo;
    private final UserXpEventRepository xpEventRepo;

    // ── Main dashboard ────────────────────────────────────────────────────────

    @Override
    public UserDashboardResponse getUserDashboard(Long userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + userId));

        // Load user content
        List<com.esprit.demo.Models.Thread> userThreads = threadRepo.findByAuthorId(userId);
        List<Post> userPosts = postRepo.findByAuthorId(userId);
        List<Comment> userComments = commentRepo.findByAuthorId(userId);

        long totalThreads   = userThreads.size();
        long totalPosts     = userPosts.size();
        long totalComments  = userComments.size();

        // Votes on user's threads
        long totalUpvotes   = voteRepo.countByThreadAuthorIdAndVoteType(userId, VoteType.UPVOTE);
        long totalDownvotes = voteRepo.countByThreadAuthorIdAndVoteType(userId, VoteType.DOWNVOTE);

        // Reactions on user's threads
        long totalLikes     = reactionRepo.countByThreadAuthorIdAndReactionType(userId, ReactionType.LIKE);
        long totalDislikes  = reactionRepo.countByThreadAuthorIdAndReactionType(userId, ReactionType.DISLIKE);

        long totalPositive  = totalUpvotes + totalLikes;
        long totalNegative  = totalDownvotes + totalDislikes;
        long totalReactions = totalPositive + totalNegative;
        long totalSaves     = wishlistRepo.countByThreadAuthorId(userId);

        double posNegRatio = totalNegative == 0
                ? totalPositive
                : round2((double) totalPositive / totalNegative);

        // Per-thread averages (votes/reactions live on threads, not posts)
        double avgUpvotes     = totalThreads == 0 ? 0 : round2((double) totalUpvotes / totalThreads);
        double avgDownvotes   = totalThreads == 0 ? 0 : round2((double) totalDownvotes / totalThreads);
        double avgReactions   = totalThreads == 0 ? 0 : round2((double) totalReactions / totalThreads);
        double engagementRate = totalThreads == 0 ? 0 : round2((double) totalReactions / totalThreads * 100);

        // Best performing thread (most upvoted)
        PostSummary bestThread = findBestThread(userId, userThreads);

        // Reaction stats map for doughnut chart
        Map<String, Long> reactionStats = new LinkedHashMap<>();
        reactionStats.put("UPVOTE",   totalUpvotes);
        reactionStats.put("DOWNVOTE", totalDownvotes);
        reactionStats.put("LIKE",     totalLikes);
        reactionStats.put("DISLIKE",  totalDislikes);

        // XP timeline
        List<XpDataPoint> xpTimeline = buildXpTimeline(userId);

        // Weekly activity
        List<DayActivity> weeklyActivity = buildWeeklyActivity(userThreads, userPosts, userComments);

        // Community comparison
        long totalAllThreads  = threadRepo.count();
        long allUpvotes       = voteRepo.countByVoteType(VoteType.UPVOTE);
        long allDownvotes     = voteRepo.countByVoteType(VoteType.DOWNVOTE);
        long allLikes         = reactionRepo.countByReactionType(ReactionType.LIKE);
        long allDislikes      = reactionRepo.countByReactionType(ReactionType.DISLIKE);
        long allReactions     = allUpvotes + allDownvotes + allLikes + allDislikes;

        double commAvgUpvotes    = totalAllThreads == 0 ? 0 : round2((double) allUpvotes / totalAllThreads);
        double commAvgReactions  = totalAllThreads == 0 ? 0 : round2((double) allReactions / totalAllThreads);
        double commAvgEngagement = totalAllThreads == 0 ? 0 : round2((double) allReactions / totalAllThreads * 100);

        // Rank & percentile
        List<Long> rankedIds = userRepo.findAllByOrderByXpDesc()
                .stream().map(User::getId).collect(Collectors.toList());
        int rankPosition = rankedIds.indexOf(userId) + 1;
        long totalUsers  = rankedIds.size();
        int percentile   = totalUsers == 0 ? 0 : (int) Math.round((double) rankPosition / totalUsers * 100);

        // Insights
        List<PerformanceInsight> insights = generateInsights(
                avgUpvotes, commAvgUpvotes,
                totalDownvotes, totalReactions, totalPositive,
                engagementRate, commAvgEngagement, totalThreads);

        return UserDashboardResponse.builder()
                .totalThreadsCreated(totalThreads)
                .totalPostsCreated(totalPosts)
                .totalCommentsCreated(totalComments)
                .totalUpvotesReceived(totalUpvotes)
                .totalDownvotesReceived(totalDownvotes)
                .totalPositiveReactions(totalPositive)
                .totalNegativeReactions(totalNegative)
                .positiveToNegativeRatio(posNegRatio)
                .totalSavesReceived(totalSaves)
                .avgUpvotesPerPost(avgUpvotes)
                .avgDownvotesPerPost(avgDownvotes)
                .avgReactionsPerPost(avgReactions)
                .bestPerformingPost(bestThread)
                .engagementRate(engagementRate)
                .reactionStats(reactionStats)
                .xpTimeline(xpTimeline)
                .weeklyActivity(weeklyActivity)
                .communityAvgUpvotesPerPost(commAvgUpvotes)
                .communityAvgReactionsPerPost(commAvgReactions)
                .communityAvgEngagementRate(commAvgEngagement)
                .userPercentile(percentile)
                .rankPosition(rankPosition)
                .performanceInsights(insights)
                .build();
    }

    @Override
    public List<XpDataPoint> getXpTimeline(Long userId) {
        return buildXpTimeline(userId);
    }

    @Override
    public List<DayActivity> getWeeklyActivity(Long userId) {
        List<com.esprit.demo.Models.Thread> threads = threadRepo.findByAuthorId(userId);
        List<Post> posts = postRepo.findByAuthorId(userId);
        List<Comment> comments = commentRepo.findByAuthorId(userId);
        return buildWeeklyActivity(threads, posts, comments);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PostSummary findBestThread(Long userId,
                                       List<com.esprit.demo.Models.Thread> threads) {
        if (threads.isEmpty()) return null;
        List<Object[]> rows = voteRepo.findUpvoteCountPerThreadForAuthor(
                userId, PageRequest.of(0, 1));
        if (rows.isEmpty()) {
            com.esprit.demo.Models.Thread first = threads.get(0);
            return PostSummary.builder()
                    .id(first.getId()).title(first.getTitle())
                    .upvotes(0).totalReactions(0).build();
        }
        Long bestId  = (Long) rows.get(0)[0];
        long upvotes = ((Number) rows.get(0)[1]).longValue();
        com.esprit.demo.Models.Thread best = threads.stream()
                .filter(t -> t.getId().equals(bestId))
                .findFirst().orElse(threads.get(0));
        long reactions = upvotes
                + voteRepo.countByThreadIdAndVoteType(best.getId(), VoteType.DOWNVOTE)
                + reactionRepo.countByThreadIdAndReactionType(best.getId(), ReactionType.LIKE)
                + reactionRepo.countByThreadIdAndReactionType(best.getId(), ReactionType.DISLIKE);
        return PostSummary.builder()
                .id(best.getId()).title(best.getTitle())
                .upvotes(upvotes).totalReactions(reactions).build();
    }

    private List<XpDataPoint> buildXpTimeline(Long userId) {
        return xpEventRepo.findTop30ByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(e -> XpDataPoint.builder()
                        .date(e.getCreatedAt().toLocalDate())
                        .xpGained(e.getAmount())
                        .newTotal(e.getNewTotal())
                        .source(e.getSourceType().name())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DayActivity> buildWeeklyActivity(
            List<com.esprit.demo.Models.Thread> threads,
            List<Post> posts,
            List<Comment> comments) {

        LocalDateTime since = LocalDateTime.now().minusWeeks(12);

        // Build ordered map of "YYYY-WW" → [threads, posts, comments]
        LinkedHashMap<String, long[]> weekMap = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime wStart = LocalDateTime.now().minusWeeks(i);
            weekMap.put(weekKey(wStart), new long[3]);
        }

        threads.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                .forEach(t -> { long[] v = weekMap.get(weekKey(t.getCreatedAt())); if (v != null) v[0]++; });
        posts.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since))
                .forEach(p -> { long[] v = weekMap.get(weekKey(p.getCreatedAt())); if (v != null) v[1]++; });
        comments.stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(since))
                .forEach(c -> { long[] v = weekMap.get(weekKey(c.getCreatedAt())); if (v != null) v[2]++; });

        return weekMap.entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("-");
                    String label = "Sem " + (parts.length > 1 ? parts[1] : e.getKey());
                    return DayActivity.builder()
                            .weekLabel(label)
                            .threadsCount(e.getValue()[0])
                            .postsCount(e.getValue()[1])
                            .commentsCount(e.getValue()[2])
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String weekKey(LocalDateTime dt) {
        int year = dt.getYear();
        int week = dt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return year + "-" + String.format("%02d", week);
    }

    private List<PerformanceInsight> generateInsights(
            double avgUpvotes, double commAvgUpvotes,
            long totalDownvotes, long totalReactions, long totalPositive,
            double engagementRate, double commAvgEngagement, long totalThreads) {

        List<PerformanceInsight> insights = new ArrayList<>();

        if (avgUpvotes > commAvgUpvotes * 1.2) {
            insights.add(PerformanceInsight.builder()
                    .type("POSITIVE").icon("\u2b06\ufe0f")
                    .message(String.format(
                            "Tes threads reçoivent en moyenne %.1f upvotes — au-dessus de la moyenne communautaire (%.1f) !",
                            avgUpvotes, commAvgUpvotes))
                    .build());
        }

        double downvoteRatio = totalReactions == 0 ? 0 : (double) totalDownvotes / totalReactions;
        if (downvoteRatio > 0.2) {
            insights.add(PerformanceInsight.builder()
                    .type("NEGATIVE").icon("\u26a0\ufe0f")
                    .message(String.format(
                            "%.0f%% de tes réactions sont négatives — relis tes contenus avant de publier.",
                            downvoteRatio * 100))
                    .build());
        }

        if (engagementRate < commAvgEngagement && totalThreads >= 3) {
            insights.add(PerformanceInsight.builder()
                    .type("SUGGESTION").icon("\ud83d\udca1")
                    .message("Enrichis tes threads avec plus de détails et de questions ouvertes pour augmenter l'engagement.")
                    .build());
        }

        double positiveRatio = totalReactions == 0 ? 0 : (double) totalPositive / totalReactions;
        if (positiveRatio > 0.85 && totalReactions > 0) {
            insights.add(PerformanceInsight.builder()
                    .type("POSITIVE").icon("\ud83d\ude0a")
                    .message(String.format(
                            "Excellent ratio positif — %.0f%% de tes réactions sont positives !",
                            positiveRatio * 100))
                    .build());
        }

        if (totalThreads > 20 && avgUpvotes > 2) {
            insights.add(PerformanceInsight.builder()
                    .type("POSITIVE").icon("\ud83c\udfc6")
                    .message("Contributeur actif et apprécié — tu fais partie des membres les plus engagés !")
                    .build());
        }

        if (totalThreads < 5) {
            insights.add(PerformanceInsight.builder()
                    .type("SUGGESTION").icon("\ud83d\udcdd")
                    .message("Participe plus activement en créant des threads pour améliorer tes statistiques.")
                    .build());
        }

        if (insights.isEmpty()) {
            insights.add(PerformanceInsight.builder()
                    .type("SUGGESTION").icon("\ud83d\ude80")
                    .message("Continue à contribuer pour débloquer des insights personnalisés !")
                    .build());
        }

        return insights;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
