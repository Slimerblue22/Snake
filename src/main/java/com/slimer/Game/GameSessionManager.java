package com.slimer.Game;

import com.slimer.Util.DebugManager;
import java.util.HashSet;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class GameSessionManager {
    private static GameSessionManager instance;
    private final HashSet<UUID> activeGames;

    // Private constructor for singleton
    private GameSessionManager() {
        activeGames = new HashSet<>();
    }

    // Get instance method for singleton
    public static GameSessionManager getInstance() {
        if (instance == null) {
            instance = new GameSessionManager();
        }
        return instance;
    }

    // Start a game for a player
    public void startGame(Player player) {
        UUID playerId = player.getUniqueId();
        if (isGameActive(player)) {
            player.sendMessage(Component.text("You already have an active game!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Attempted to start a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but a game was already active.");
            return; // Game already active
        }
        activeGames.add(playerId);
        player.sendMessage(Component.text("Your game has started!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Game started for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }

    // Stop a player's game
    public void stopGame(Player player) {
        UUID playerId = player.getUniqueId();
        if (!isGameActive(player)) {
            player.sendMessage(Component.text("You don't have an active game to stop!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Attempted to stop a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but no game was active.");
            return; // No game to stop
        }
        activeGames.remove(playerId);
        player.sendMessage(Component.text("Your game has been stopped!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Game stopped for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }

    // Check if a player has an active game
    public boolean isGameActive(Player player) {
        return activeGames.contains(player.getUniqueId());
    }
}