package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private int streakDays;

    // Gamification
    private long xp;
    private int level;
    private String levelTitle;

    // Barre de progression vers le prochain niveau
    private long xpCurrentLevelMin;  // XP minimum du niveau actuel
    private long xpNextLevelMin;     // XP minimum du prochain niveau (-1 si max)
    private long xpProgress;         // XP accumulé dans ce niveau
    private long xpNeeded;           // XP total pour finir ce niveau
    private double progressPercent;  // % de remplissage de la barre (0-100)
    private boolean isMaxLevel;
}
