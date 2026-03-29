package com.ybrainy.backend.dto.recommendation;

import java.time.Instant;
import java.util.List;

public record RecommendationSummaryResponseDTO(
        String sourceFile,
        Instant generatedAt,
        ForecastContextDTO forecastContext,
        List<RecommendationItemDTO> topRecommendations,
        List<ImprovementActionDTO> improvementActions
) {
    public record ForecastContextDTO(
            Double marginPct,
            Double marginTrendPct,
            String topRevenueSource,
            String watchCategory,
            Double watchGrowthPct,
            ForecastHorizonDTO forecastHorizon
    ) {
    }

    public record ForecastHorizonDTO(
            String from,
            String to,
            Integer monthsAhead,
            Double incomeTotal,
            Double expensesTotal,
            Double profitTotal
    ) {
    }

    public record RecommendationItemDTO(
            Integer recommendationRank,
            String title,
            String platform,
            String skillCategory,
            Double hybridFinalScore,
            String confidenceLevel,
            String impactBand,
            String recommendationReason,
            String url
    ) {
    }

    public record ImprovementActionDTO(
            String priority,
            String action,
            String rationale,
            String targetMetric
    ) {
    }
}
