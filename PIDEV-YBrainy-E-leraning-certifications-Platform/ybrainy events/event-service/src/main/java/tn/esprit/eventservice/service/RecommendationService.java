package tn.esprit.eventservice.service;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.esprit.eventservice.client.AiEmbeddingClient;
import tn.esprit.eventservice.client.FeedbackClient;
import tn.esprit.eventservice.client.InscriptionClient;
import tn.esprit.eventservice.dto.EventStatsDto;
import tn.esprit.eventservice.dto.FeedbackDto;
import tn.esprit.eventservice.dto.RecommendedEventDto;
import tn.esprit.eventservice.entity.Event;
import tn.esprit.eventservice.entity.EventStatut;
import tn.esprit.eventservice.entity.EventType;
import tn.esprit.eventservice.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    private final EventRepository eventRepository;
    private final InscriptionClient inscriptionClient;
    private final FeedbackClient feedbackClient;
    private final AiEmbeddingClient aiEmbeddingClient;

    public List<RecommendedEventDto> getRecommendationsForStudent(long studentId, int limit) {
        // 1. Gather Candidate Events
        List<Event> allEvents = (List<Event>) eventRepository.findAll();
        List<Long> enrolledEventIds = safeGetEnrolledEventIds(studentId);

        List<Event> candidateEvents = allEvents.stream()
                .filter(e -> e.getStatut() != EventStatut.ANNULE && e.getStatut() != EventStatut.TERMINE)
                .filter(e -> e.getDateDebut() != null && e.getDateDebut().isAfter(LocalDateTime.now()))
                .filter(e -> !enrolledEventIds.contains(e.getIdEvent()))
                .collect(Collectors.toList());

        if (candidateEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Gather Student History
        List<FeedbackDto> studentFeedbacks = safeGetStudentFeedbacks(studentId);
        
        Set<EventType> preferredTypes = new HashSet<>();
        List<String> positiveHistoryDescriptions = new ArrayList<>();

        // Add history from inscriptions
        for (long eventId : enrolledEventIds) {
            eventRepository.findById(eventId).ifPresent(e -> {
                preferredTypes.add(e.getType());
                if (e.getDescription() != null && !e.getDescription().isBlank()) {
                    positiveHistoryDescriptions.add(e.getDescription());
                }
            });
        }

        // Add history from feedbacks
        for (FeedbackDto f : studentFeedbacks) {
            if (f.getRating() >= 4) {
                eventRepository.findById(f.getEventId()).ifPresent(e -> {
                    preferredTypes.add(e.getType());
                    if (e.getDescription() != null && !e.getDescription().isBlank()) {
                        if(!positiveHistoryDescriptions.contains(e.getDescription())){
                             positiveHistoryDescriptions.add(e.getDescription());
                        }
                    }
                });
            }
        }

        // 3. Fallback: Cold Start Check
        boolean isColdStart = preferredTypes.isEmpty() && positiveHistoryDescriptions.isEmpty();
        if (isColdStart) {
            return fallbackTopRatedEvents(candidateEvents, limit);
        }

        // 4. Compute AI Profile Embedding (Average of all positive history embeddings)
        List<Double> profileEmbedding = computeProfileEmbedding(positiveHistoryDescriptions);

        // 5. Score Candidates
        List<RecommendedEventDto> scoredCandidates = new ArrayList<>();
        
        for (Event candidate : candidateEvents) {
            // Signal 1: Collab / Rule-based (0.0 to 1.0)
            double collabScore = preferredTypes.contains(candidate.getType()) ? 1.0 : 0.0;

            // Signal 2: Content-based (Average Rating -> 0.0 to 1.0)
            EventStatsDto stats = safeGetStats(candidate.getIdEvent());
            double contentScore = 0.0;
            if (stats != null && stats.getAverageRating() > 0) {
                contentScore = stats.getAverageRating() / 5.0; 
            }

            // Signal 3: AI Semantic Similarity (0.0 to 1.0)
            double aiScore = 0.0;
            if (profileEmbedding != null && !profileEmbedding.isEmpty() && candidate.getDescription() != null) {
                List<Double> candidateEmbedding = aiEmbeddingClient.getEmbeddingSync(candidate.getDescription());
                aiScore = aiEmbeddingClient.computeCosineSimilarity(profileEmbedding, candidateEmbedding);
                // Clamp to [0,1] just in case
                aiScore = Math.max(0.0, Math.min(1.0, aiScore));
            }

            // Weights
            double WEIGH_COLLAB = 0.3;
            double WEIGH_CONTENT = 0.3;
            double WEIGH_AI = 0.4;

            double finalScore = (collabScore * WEIGH_COLLAB) + (contentScore * WEIGH_CONTENT) + (aiScore * WEIGH_AI);

            String reason = generateReason(collabScore, contentScore, aiScore, candidate.getType());

            scoredCandidates.add(RecommendedEventDto.builder()
                    .idEvent(candidate.getIdEvent())
                    .name(candidate.getName())
                    .type(candidate.getType())
                    .dateDebut(candidate.getDateDebut())
                    .location(candidate.getLocation())
                    .description(candidate.getDescription())
                    .recommendationScore(finalScore)
                    .recommendationReason(reason)
                    .build());
        }

        // 6. Sort and Return
        scoredCandidates.sort((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()));
        return scoredCandidates.stream().limit(limit).collect(Collectors.toList());
    }

    private List<RecommendedEventDto> fallbackTopRatedEvents(List<Event> candidateEvents, int limit) {
        List<RecommendedEventDto> scored = new ArrayList<>();
        for (Event candidate : candidateEvents) {
            EventStatsDto stats = safeGetStats(candidate.getIdEvent());
            double score = (stats != null && stats.getAverageRating() > 0) ? (stats.getAverageRating() / 5.0) : 0.0;
            
            scored.add(RecommendedEventDto.builder()
                    .idEvent(candidate.getIdEvent())
                    .name(candidate.getName())
                    .type(candidate.getType())
                    .dateDebut(candidate.getDateDebut())
                    .location(candidate.getLocation())
                    .description(candidate.getDescription())
                    .recommendationScore(score)
                    .recommendationReason("Top rated event on YBrainy!")
                    .build());
        }
        scored.sort((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()));
        return scored.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Double> computeProfileEmbedding(List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) return null;
        
        List<List<Double>> allEmbeddings = new ArrayList<>();
        // Take max 5 descriptions to avoid rate limits / long delays
        int limit = Math.min(5, descriptions.size());
        for (int i = 0; i < limit; i++) {
            List<Double> emb = aiEmbeddingClient.getEmbeddingSync(descriptions.get(i));
            if (emb != null && !emb.isEmpty()) {
                allEmbeddings.add(emb);
            }
        }
        
        if (allEmbeddings.isEmpty()) return null;

        // Average the embeddings
        int dimension = allEmbeddings.get(0).size();
        List<Double> avg = new ArrayList<>(Collections.nCopies(dimension, 0.0));
        
        for (List<Double> emb : allEmbeddings) {
            for (int i = 0; i < dimension; i++) {
                avg.set(i, avg.get(i) + emb.get(i));
            }
        }
        for (int i = 0; i < dimension; i++) {
            avg.set(i, avg.get(i) / allEmbeddings.size());
        }
        return avg;
    }

    private String generateReason(double collabScore, double contentScore, double aiScore, EventType type) {
        if (aiScore > 0.8) {
            return "Highly relevant to your recent interests.";
        } else if (collabScore > 0) {
            return "Because you matched with " + type + " events.";
        } else if (contentScore > 0.8) {
            return "Highly rated by other students.";
        }
        return "Recommended based on your profile.";
    }

    // --- Safe Feign Calls with Fallbacks --- //

    private List<Long> safeGetEnrolledEventIds(long studentId) {
        try {
            List<Long> ids = inscriptionClient.getRegisteredEventIdsByStudent(studentId);
            return ids != null ? ids : new ArrayList<>();
        } catch (Exception e) {
            logger.warn("Inscription service unreachable: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<FeedbackDto> safeGetStudentFeedbacks(long studentId) {
        try {
            List<FeedbackDto> feedbacks = feedbackClient.getFeedbacksByStudent(studentId);
            return feedbacks != null ? feedbacks : new ArrayList<>();
        } catch (Exception e) {
            logger.warn("Feedback service unreachable: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private EventStatsDto safeGetStats(long eventId) {
        try {
             return feedbackClient.getStatsByEvent(eventId);
        } catch (Exception e) {
            logger.debug("Feedback stats not found or service unreachable for event {}", eventId);
            return null;
        }
    }
}
