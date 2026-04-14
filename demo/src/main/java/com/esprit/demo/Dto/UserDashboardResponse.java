package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardResponse {

    // ── Overview ─────────────────────────────────────────────────────────────
    private long totalThreadsCreated;
    private long totalPostsCreated;
    private long totalCommentsCreated;
    private long totalUpvotesReceived;
    private long totalDownvotesReceived;
    private long totalPositiveReactions;
    private long totalNegativeReactions;
    private double positiveToNegativeRatio;
    private long totalSavesReceived;

    // ── Performance metrics ───────────────────────────────────────────────────
    private double avgUpvotesPerPost;
    private double avgDownvotesPerPost;
    private double avgReactionsPerPost;
    private PostSummary bestPerformingPost;
    private double engagementRate;

    // ── Charts data ───────────────────────────────────────────────────────────
    private Map<String, Long> reactionStats;
    private List<XpDataPoint> xpTimeline;
    private List<DayActivity> weeklyActivity;

    // ── Community comparison ──────────────────────────────────────────────────
    private double communityAvgUpvotesPerPost;
    private double communityAvgReactionsPerPost;
    private double communityAvgEngagementRate;
    private int userPercentile;
    private int rankPosition;

    // ── Insights ─────────────────────────────────────────────────────────────
    private List<PerformanceInsight> performanceInsights;
}
