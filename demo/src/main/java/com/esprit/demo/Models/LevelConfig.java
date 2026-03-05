package com.esprit.demo.Models;

/**
 * Système de niveaux — progression EXPONENTIELLE.
 *
 * Level │ Titre           │ XP Min   │ XP pour passer au suivant
 * ──────┼─────────────────┼──────────┼───────────────────────────
 *   1   │ 🌱 Newcomer     │      0   │         100 XP
 *   2   │ 📚 Explorer     │    101   │         400 XP  (x4)
 *   3   │ ⚡ Contributor  │    501   │       1 000 XP  (x2.5)
 *   4   │ 🔥 Active Member│   1501   │       3 500 XP  (x3.5)
 *   5   │ 💡 Helper       │   5001   │      10 000 XP  (x2.9)
 *   6   │ 🛡️ Trusted      │  15001   │      25 000 XP  (x2.5)
 *   7   │ 🏅 Expert       │  40001   │      60 000 XP  (x2.4)
 *   8   │ 👑 Legend       │ 100001   │           ∞
 *
 * Chaque niveau est significativement plus difficile que le précédent.
 */

public class LevelConfig {
    // minXP pour atteindre ce niveau (index 0 = level 1)
    private static final long[] MIN_XP = {
            0,      // Level 1 - Newcomer
            101,    // Level 2 - Explorer
            501,    // Level 3 - Contributor
            1501,   // Level 4 - Active Member
            5001,   // Level 5 - Helper
            15001,  // Level 6 - Trusted
            40001,  // Level 7 - Expert
            100001  // Level 8 - Legend
    };

    private static final String[] TITLES = {
            "🌱 Newcomer",
            "📚 Explorer",
            "⚡ Contributor",
            "🔥 Active Member",
            "💡 Helper",
            "🛡️ Trusted",
            "🏅 Expert",
            "👑 Legend"
    };

    public static final int MAX_LEVEL = 8;

    /** Calcule le niveau à partir du total XP */
    public static int computeLevel(long xp) {
        int level = 1;
        for (int i = MIN_XP.length - 1; i >= 0; i--) {
            if (xp >= MIN_XP[i]) {
                level = i + 1;
                break;
            }
        }
        return level;
    }

    /** XP minimum requis pour ce niveau (1-based) */
    public static long minXpForLevel(int level) {
        if (level < 1) return 0;
        if (level > MAX_LEVEL) return MIN_XP[MAX_LEVEL - 1];
        return MIN_XP[level - 1];
    }

    /** XP minimum du niveau suivant (-1 si déjà au max) */
    public static long minXpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return -1;
        return MIN_XP[currentLevel]; // index currentLevel = level+1 - 1
    }

    /** Titre du niveau */
    public static String getTitle(int level) {
        if (level < 1 || level > MAX_LEVEL) return TITLES[0];
        return TITLES[level - 1];
    }
}
