package com.esprit.demo.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpAwardResult {
    // Cet événement
    private int xpAwarded;
    private String description;

    // État actuel
    private long totalXp;
    private int currentLevel;
    private String levelTitle;

    // Level-up ?
    private boolean leveledUp;
    private int previousLevel;
    private String previousLevelTitle;

    // Barre de progression
    private long xpCurrentLevelMin;  // XP minimum du niveau actuel
    private long xpNextLevelMin;     // XP minimum du prochain niveau (-1 si max)
    private long xpProgress;         // XP accumulé dans le niveau actuel
    private long xpNeeded;           // XP total pour finir le niveau actuel
    private double progressPercent;  // % de remplissage de la barre

    private boolean isMaxLevel;
}
