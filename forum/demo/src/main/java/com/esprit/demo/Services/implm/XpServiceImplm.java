package com.esprit.demo.Services.implm;

import com.esprit.demo.Dto.UserProfileResponse;
import com.esprit.demo.Dto.XpAwardResult;
import com.esprit.demo.Dto.XpEventResponse;
import com.esprit.demo.Exceptions.ResourceNotFoundException;
import com.esprit.demo.Models.LevelConfig;
import com.esprit.demo.Models.User;
import com.esprit.demo.Models.UserXpEvent;
import com.esprit.demo.Models.XpSource;
import com.esprit.demo.Repositories.UserRepository;
import com.esprit.demo.Repositories.UserXpEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class XpServiceImplm {
    // ── Constantes XP selon les specs FR-4.1 ──────────────────────
    public static final int XP_THREAD_CREATED           = 20;
    public static final int XP_POST_CREATED             = 10;
    public static final int XP_POST_DETAILED            = 30;   // > 200 mots
    public static final int XP_COMMENT_CREATED          = 5;
    public static final int XP_UPVOTE_RECEIVED          = 5;
    public static final int XP_BEST_ANSWER              = 50;
    public static final int DETAILED_WORD_THRESHOLD     = 200;

    private final UserRepository userRepository;
    private final UserXpEventRepository xpEventRepository;

    // ── Méthode principale ─────────────────────────────────────────

    @Transactional
    public XpAwardResult awardXp(Long userId, int amount, XpSource source,
                                 Long referenceId, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'id : " + userId));

        int previousLevel    = user.getLevel();
        long newTotal        = user.getXp() + amount;
        int newLevel         = LevelConfig.computeLevel(newTotal);

        // Mise à jour de l'utilisateur
        user.setXp(newTotal);
        user.setLevel(newLevel);
        userRepository.save(user);

        UserXpEvent event = UserXpEvent.builder()
                .user(user)
                .sourceType(source)
                .amount(amount)
                .referenceId(referenceId)
                .newTotal(newTotal)
                .newLevel(newLevel)
                .description(description)
                .build();
        xpEventRepository.save(event);

        return buildResult(amount, newTotal, newLevel, previousLevel, description);
    }


    @Transactional
    public XpAwardResult awardThreadCreated(Long userId, Long threadId) {
        return awardXp(userId, XP_THREAD_CREATED, XpSource.THREAD_CREATED,
                threadId, "+20 XP — Thread créé");
    }

    @Transactional
    public XpAwardResult awardPostCreated(Long userId, Long postId, String body) {
        int words = countWords(body);
        if (words > DETAILED_WORD_THRESHOLD) {
            return awardXp(userId, XP_POST_DETAILED, XpSource.POST_DETAILED,
                    postId, "+30 XP — Réponse détaillée (" + words + " mots)");
        }
        return awardXp(userId, XP_POST_CREATED, XpSource.POST_CREATED,
                postId, "+10 XP — Réponse créée");
    }

    @Transactional
    public XpAwardResult awardCommentCreated(Long userId, Long commentId) {
        return awardXp(userId, XP_COMMENT_CREATED, XpSource.COMMENT_CREATED,
                commentId, "+5 XP — Commentaire créé");
    }

    @Transactional
    public XpAwardResult awardUpvoteReceived(Long userId, Long postId) {
        return awardXp(userId, XP_UPVOTE_RECEIVED, XpSource.UPVOTE_RECEIVED,
                postId, "+5 XP — Upvote reçu");
    }

    @Transactional
    public XpAwardResult awardBestAnswer(Long userId, Long postId) {
        return awardXp(userId, XP_BEST_ANSWER, XpSource.BEST_ANSWER,
                postId, "+50 XP — Meilleure réponse sélectionnée !");
    }

    // ── Profil & Historique ────────────────────────────────────────

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'id : " + userId));
        return buildProfile(user);
    }

    public List<XpEventResponse> getXpHistory(Long userId) {
        return xpEventRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toEventResponse).collect(Collectors.toList());
    }

    public List<XpEventResponse> getRecentXp(Long userId) {
        return xpEventRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toEventResponse).collect(Collectors.toList());
    }

    public List<UserProfileResponse> getLeaderboard() {
        return userRepository.findTop10ByOrderByXpDesc()
                .stream().map(this::buildProfile).collect(Collectors.toList());
    }

    // ── Helpers privés ─────────────────────────────────────────────

    private XpAwardResult buildResult(int awarded, long totalXp, int level,
                                      int previousLevel, String description) {
        boolean isMax       = (level >= LevelConfig.MAX_LEVEL);
        long currentMin     = LevelConfig.minXpForLevel(level);
        long nextMin        = LevelConfig.minXpForNextLevel(level); // -1 si max
        long xpProgress     = totalXp - currentMin;
        long xpNeeded       = isMax ? 0 : nextMin - currentMin;
        double percent      = isMax ? 100.0
                : Math.min(100.0, (xpProgress * 100.0) / xpNeeded);

        return XpAwardResult.builder()
                .xpAwarded(awarded)
                .description(description)
                .totalXp(totalXp)
                .currentLevel(level)
                .levelTitle(LevelConfig.getTitle(level))
                .leveledUp(level > previousLevel)
                .previousLevel(previousLevel)
                .previousLevelTitle(LevelConfig.getTitle(previousLevel))
                .xpCurrentLevelMin(currentMin)
                .xpNextLevelMin(nextMin)
                .xpProgress(xpProgress)
                .xpNeeded(xpNeeded)
                .progressPercent(percent)
                .isMaxLevel(isMax)
                .build();
    }

    public UserProfileResponse buildProfile(User user) {
        int level       = user.getLevel();
        long xp         = user.getXp();
        boolean isMax   = (level >= LevelConfig.MAX_LEVEL);
        long currentMin = LevelConfig.minXpForLevel(level);
        long nextMin    = LevelConfig.minXpForNextLevel(level);
        long progress   = xp - currentMin;
        long needed     = isMax ? 0 : nextMin - currentMin;
        double percent  = isMax ? 100.0
                : Math.min(100.0, (progress * 100.0) / needed);

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .xp(xp)
                .level(level)
                .levelTitle(LevelConfig.getTitle(level))
                .streakDays(user.getStreakDays())
                .xpCurrentLevelMin(currentMin)
                .xpNextLevelMin(nextMin)
                .xpProgress(progress)
                .xpNeeded(needed)
                .progressPercent(percent)
                .isMaxLevel(isMax)
                .build();
    }

    private XpEventResponse toEventResponse(UserXpEvent e) {
        return XpEventResponse.builder()
                .id(e.getId())
                .sourceType(e.getSourceType())
                .amount(e.getAmount())
                .newTotal(e.getNewTotal())
                .newLevel(e.getNewLevel())
                .description(e.getDescription())
                .levelUp(e.isLevelUp())
                .createdAt(e.getCreatedAt())
                .build();
    }

    /** Compte les mots dans un texte */
    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}
