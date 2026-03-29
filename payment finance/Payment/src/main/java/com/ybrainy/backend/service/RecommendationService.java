package com.ybrainy.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ybrainy.backend.dto.recommendation.RecommendationSummaryResponseDTO;
import com.ybrainy.backend.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ObjectMapper objectMapper;

    @Value("${recommendations.output-directory:D:/4eme/project pi/spring/PIDEV-YBrainy-E-leraning-certifications-Platform/Recommendations/Recommendations output}")
    private String recommendationsOutputDirectory;

    @Value("${recommendations.summary-file-name:recommendations_hybrid_summary.json}")
    private String recommendationsSummaryFileName;

    public RecommendationSummaryResponseDTO getSummary(int limit) {
        if (limit <= 0) {
            throw new BusinessRuleException("Limit must be greater than zero.");
        }

        Path summaryPath = resolveSummaryPath();
        JsonNode root = readSummaryJson(summaryPath);

        return new RecommendationSummaryResponseDTO(
                summaryPath.toString(),
                fileModifiedAt(summaryPath),
                parseForecastContext(root.path("forecast_context")),
                parseTopRecommendations(root.path("top_recommendations"), limit),
                parseImprovementActions(root.path("improvement_actions"))
        );
    }

    private Path resolveSummaryPath() {
        Path outputDir = Paths.get(recommendationsOutputDirectory).toAbsolutePath().normalize();
        if (!Files.isDirectory(outputDir)) {
            throw new BusinessRuleException("Recommendations output directory not found: " + outputDir);
        }

        Path summaryPath = outputDir.resolve(recommendationsSummaryFileName).normalize();
        if (!Files.isRegularFile(summaryPath)) {
            throw new BusinessRuleException("Recommendations summary file not found: " + summaryPath);
        }
        return summaryPath;
    }

    private JsonNode readSummaryJson(Path summaryPath) {
        try {
            return objectMapper.readTree(Files.newInputStream(summaryPath));
        } catch (IOException ex) {
            throw new BusinessRuleException("Failed to read recommendations summary: " + ex.getMessage());
        }
    }

    private Instant fileModifiedAt(Path filePath) {
        try {
            return Files.getLastModifiedTime(filePath).toInstant();
        } catch (IOException ex) {
            return Instant.now();
        }
    }

    private RecommendationSummaryResponseDTO.ForecastContextDTO parseForecastContext(JsonNode node) {
        JsonNode horizon = node.path("forecast_horizon");
        RecommendationSummaryResponseDTO.ForecastHorizonDTO horizonDTO =
                new RecommendationSummaryResponseDTO.ForecastHorizonDTO(
                        textOrNull(horizon, "from"),
                        textOrNull(horizon, "to"),
                        intOrNull(horizon, "months_ahead"),
                        doubleOrNull(horizon, "income_total"),
                        doubleOrNull(horizon, "expenses_total"),
                        doubleOrNull(horizon, "profit_total")
                );

        return new RecommendationSummaryResponseDTO.ForecastContextDTO(
                doubleOrNull(node, "margin_pct"),
                doubleOrNull(node, "margin_trend_pct"),
                textOrNull(node, "top_revenue_source"),
                textOrNull(node, "watch_category"),
                doubleOrNull(node, "watch_growth_pct"),
                horizonDTO
        );
    }

    private List<RecommendationSummaryResponseDTO.RecommendationItemDTO> parseTopRecommendations(JsonNode node, int limit) {
        List<RecommendationSummaryResponseDTO.RecommendationItemDTO> items = new ArrayList<>();
        if (!node.isArray()) {
            return items;
        }

        int count = 0;
        for (JsonNode item : node) {
            if (count++ >= limit) {
                break;
            }
            items.add(new RecommendationSummaryResponseDTO.RecommendationItemDTO(
                    intOrNull(item, "recommendation_rank"),
                    textOrNull(item, "title"),
                    textOrNull(item, "platform"),
                    textOrNull(item, "skill_category"),
                    doubleOrNull(item, "hybrid_final_score"),
                    textOrNull(item, "confidence_level"),
                    textOrNull(item, "impact_band"),
                    textOrNull(item, "recommendation_reason"),
                    textOrNull(item, "url")
            ));
        }
        return items;
    }

    private List<RecommendationSummaryResponseDTO.ImprovementActionDTO> parseImprovementActions(JsonNode node) {
        List<RecommendationSummaryResponseDTO.ImprovementActionDTO> actions = new ArrayList<>();
        if (!node.isArray()) {
            return actions;
        }

        for (JsonNode item : node) {
            actions.add(new RecommendationSummaryResponseDTO.ImprovementActionDTO(
                    textOrNull(item, "priority"),
                    textOrNull(item, "action"),
                    textOrNull(item, "rationale"),
                    textOrNull(item, "target_metric")
            ));
        }
        return actions;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private Double doubleOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isInt() || value.isLong() ? value.asInt() : null;
    }
}
