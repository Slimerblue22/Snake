package com.slimer.Game;

import com.slimer.Util.DebugManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameSessionManager {
    private static GameSessionManager instance;
    private final HashMap<UUID, Map<String, Location>> activeGames;

    // Private constructor for singleton
    private GameSessionManager() {
        activeGames = new HashMap<>();
    }

    // Get instance method for singleton
    public static GameSessionManager getInstance() {
        if (instance == null) {
            instance = new GameSessionManager();
        }
        return instance;
    }

    // Check if a player has an active game
    public boolean isGameActive(Player player) {
        if (player == null) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Player object is null in isGameActive method.");
            return false;
        }
        return activeGames.containsKey(player.getUniqueId());
    }

    // Start a game for a player
    public void startGame(Player player, Location gameTeleportLocation, Location lobbyTeleportLocation) {
        if (player == null || gameTeleportLocation == null || lobbyTeleportLocation == null) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Null parameter(s) provided in startGame method.");
            return;
        }

        if (isGameActive(player)) {
            player.sendMessage(Component.text("You already have an active game!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Attempted to start a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but a game was already active.");
            return;
        }

        // Adding player to the active games list
        HashMap<String, Location> locations = new HashMap<>();
        locations.put("game", gameTeleportLocation);
        locations.put("lobby", lobbyTeleportLocation);
        activeGames.put(player.getUniqueId(), locations);

        // Teleporting the player to the game location
        if (!player.teleport(gameTeleportLocation)) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Failed to teleport player " + player.getName() + " to game location.");
        }

        // Informing the player that the game has started
        player.sendMessage(Component.text("Your game has started!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Game started for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }

    // Stop a player's game
    public void stopGame(Player player) {
        if (player == null) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Player object is null in stopGame method.");
            return;
        }

        if (!isGameActive(player)) {
            player.sendMessage(Component.text("You don't have an active game to stop!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Attempted to stop a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but no game was active.");
            return;
        }

        // Retrieving game locations and teleporting player to the lobby
        Map<String, Location> locations = activeGames.get(player.getUniqueId());
        if (locations != null && locations.get("lobby") != null) {
            player.teleport(locations.get("lobby"));
        } else {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Lobby location is null or locations map is null for player " + player.getName() + " with UUID of " + player.getUniqueId());
        }

        // Removing player from the active games list
        activeGames.remove(player.getUniqueId());

        // Informing the player that their game has been stopped
        player.sendMessage(Component.text("Your game has been stopped!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Game stopped for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }
}