package com.slimer.Game;

import com.slimer.Util.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.UUID;

public class ScoreManager {
    private final HashMap<UUID, BossBar> playerBossBars;
    private final HashMap<UUID, Integer> currentScores;

    public ScoreManager() {
        this.playerBossBars = new HashMap<>();
        this.currentScores = new HashMap<>();
    }

    public void startScore(Player player) {
        int highScore = PlayerData.getInstance().getHighScore(player);
        BossBar bossBar = BossBar.bossBar(
                Component.text("Score: 0 | High Score: " + highScore),
                0.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );

        player.showBossBar(bossBar);
        playerBossBars.put(player.getUniqueId(), bossBar);
        currentScores.put(player.getUniqueId(), 0); // Initialize score
    }

    public void updateScore(Player player) {
        UUID playerId = player.getUniqueId();
        BossBar bossBar = playerBossBars.get(playerId);
        if (bossBar != null) {
            // Increment the current score
            int currentScore = currentScores.getOrDefault(playerId, 0) + 1;
            int highScore = PlayerData.getInstance().getHighScore(player);

            // Calculate the progress
            float progress = highScore > 0 ? (float) currentScore / highScore : 1.0f;
            progress = Math.min(progress, 1.0f); // Ensure progress does not exceed 1

            // Update the boss bar
            bossBar.name(Component.text("Score: " + currentScore + " | High Score: " + highScore));
            bossBar.progress(progress);

            // Update the current score
            currentScores.put(playerId, currentScore);
        }
    }

    public void stopScore(Player player) {
        UUID playerId = player.getUniqueId();
        BossBar bossBar = playerBossBars.remove(playerId);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }

        Integer finalScore = currentScores.remove(playerId);
        if (finalScore != null) {
            PlayerData.getInstance().setHighScore(player, finalScore);
        }
    }
}