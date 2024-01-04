package com.slimer.Game;

import com.slimer.Game.AppleManagement.AppleCollection;
import com.slimer.Game.AppleManagement.AppleLifecycle;
import com.slimer.Game.Listeners.PlayerInputListener;
import com.slimer.Game.SnakeManagement.SnakeLifecycle;
import com.slimer.Game.SnakeManagement.SnakeMovement;
import com.slimer.Main;
import com.slimer.Region.RegionService;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the logic for starting and stopping game sessions.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class GameManager {
    private final HashMap<UUID, Map<String, Object>> activeGames;
    private final SnakeLifecycle snakeLifecycle;
    private final PlayerInputListener playerInputListener;
    private final ScoreManager scoreManager;
    private final SnakeMovement snakeMovement;
    private final Main main;
    private final AppleLifecycle appleLifecycle;
    private final AppleCollection appleCollection;
    private final Map<Player, BukkitRunnable> movementTasks = new HashMap<>();
    private final Map<Player, BukkitRunnable> appleCollectionTasks = new HashMap<>();
    private static final String ACTIVE_GAME_EXISTS_MSG = "You already have an active game!";
    private static final String NO_ACTIVE_GAME_MSG = "You don't have an active game to stop!";
    private static final String GAME_START_MSG = "Your game has started!";
    private static final String GAME_STOP_MSG = "Your game has been stopped!";
    private static final String NOT_IN_LOBBY_MSG = "You must be within a lobby region to start the game.";
    private static final String LOBBY_NOT_LINKED_MSG = "The lobby you are in is not properly linked to a game region. You cannot start the game.";
    private static final String TELEPORT_LOCATION_MISSING_MSG = "Could not find the teleport location for the game or lobby region.";
    private static final String GAME_STOP_ATTEMPT_LOG = "Attempted to stop a game for %s but no game was active.";
    private static final String GAME_STARTED_LOG = "Game started for player: %s";
    private static final String GAME_STOPPED_LOG = "Game stopped for player: %s";
    private static final String GAME_STOP_ON_SHUTDOWN_LOG = "Detected active game for player: %s during shutdown. Stopping game!";
    private static final String GAME_DATA_LOG = "Game Data for Player %s: %s";

    /**
     * Constructor for GameManager.
     */
    public GameManager(SnakeLifecycle snakeLifecycle, PlayerInputListener playerInputListener, ScoreManager scoreManager, SnakeMovement snakeMovement, Main main, AppleLifecycle appleLifecycle, AppleCollection appleCollection) {
        this.snakeLifecycle = snakeLifecycle;
        this.playerInputListener = playerInputListener;
        this.scoreManager = scoreManager;
        this.snakeMovement = snakeMovement;
        this.main = main;
        this.appleLifecycle = appleLifecycle;
        this.appleCollection = appleCollection;
        activeGames = new HashMap<>();
    }

    /**
     * Checks if the specified player has an active game.
     * Currently only used by {@code PlayerDisconnectListener}.
     * Calls from within this class interact with the hashmap directly.
     *
     * @param player The player to check for an active game.
     * @return true if the player has an active game, false otherwise.
     */
    public boolean hasActiveGame(Player player) {
        return player != null && activeGames.containsKey(player.getUniqueId());
    }

    public void startGame(Player player) {
        // Run pregame checks
        HashMap<String, Object> gameData = performPregameChecks(player);
        if (gameData == null) {
            return; // Pregame checks failed, stop the method
        }

        // Adding player to the active games list
        activeGames.put(player.getUniqueId(), gameData);
        Location gameLocation = (Location) gameData.get("gameLocation");
        String gameRegionName = (String) gameData.get("gameRegionName");
        String worldName = player.getWorld().getName();

        // Teleporting the player to the game
        player.teleport(gameLocation);

        // Spawn snake for player
        snakeLifecycle.spawnSnakeForPlayer(player, gameLocation);

        // Start monitoring player inputs
        playerInputListener.addPlayer(player);

        // Start movement task here using bukkit repeating tasks
        BukkitRunnable movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                snakeMovement.moveSnake(player);
            }
        };
        movementTask.runTaskTimer(main, 0L, 0L);
        movementTasks.put(player, movementTask);

        // Start scoring for player
        scoreManager.startScore(player);

        // Place an apple
        appleLifecycle.createAppleForPlayer(player, worldName, gameRegionName);

        // Start apple collection service
        BukkitRunnable appleCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                appleCollection.collectApple(player, worldName, gameRegionName);
            }
        };
        appleCollectionTask.runTaskTimer(main, 0L, 0L);
        appleCollectionTasks.put(player, appleCollectionTask);

        // Play game start sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        // Informing the player that the game has started
        player.sendMessage(Component.text(GAME_START_MSG, NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.GAME_MANAGER, String.format(GAME_STARTED_LOG, player.getName()));
    }

    private HashMap<String, Object> performPregameChecks(Player player) {
        // Checking if player already has an active game
        if (activeGames.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text(ACTIVE_GAME_EXISTS_MSG, NamedTextColor.RED));
            return null;
        }

        // Check if player is inside a lobby region
        RegionService regionService = RegionService.getInstance();

        String currentLobbyRegion = WGHelpers.getPlayerCurrentRegion(player);
        boolean isRegistered = (currentLobbyRegion != null) && regionService.isRegionRegistered(currentLobbyRegion);
        String regionType = isRegistered ? regionService.getRegionType(currentLobbyRegion) : null;

        if (!"lobby".equals(regionType)) {
            player.sendMessage(Component.text(NOT_IN_LOBBY_MSG, NamedTextColor.RED));
            return null;
        }

        // Check if lobby region is linked
        boolean isLinked = regionService.isRegionLinked(currentLobbyRegion);
        String currentGameRegion = regionService.getLinkedRegion(currentLobbyRegion);

        if (!isLinked || currentGameRegion == null) {
            player.sendMessage(Component.text(LOBBY_NOT_LINKED_MSG, NamedTextColor.RED));
            return null;
        }

        // Check for unexpected null locations
        World gameWorld = regionService.getRegionWorld(currentGameRegion);
        World lobbyWorld = regionService.getRegionWorld(currentLobbyRegion);

        Location gameTeleportLocation = regionService.getRegionTeleportLocation(currentGameRegion, gameWorld);
        Location lobbyTeleportLocation = regionService.getRegionTeleportLocation(currentLobbyRegion, lobbyWorld);

        if (gameTeleportLocation == null || lobbyTeleportLocation == null) {
            player.sendMessage(Component.text(TELEPORT_LOCATION_MISSING_MSG, NamedTextColor.RED));
            return null;
        }

        HashMap<String, Object> gameData = new HashMap<>();
        gameData.put("gameLocation", gameTeleportLocation);
        gameData.put("lobbyLocation", lobbyTeleportLocation);
        gameData.put("gameRegionName", currentGameRegion);
        gameData.put("lobbyRegionName", currentLobbyRegion);

        // Debug line
        DebugManager.log(DebugManager.Category.GAME_MANAGER, String.format(GAME_DATA_LOG, player.getName(), gameData));

        return gameData;
    }

    public void stopGame(Player player) {
        // Checking if player has a game to stop
        if (!activeGames.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text(NO_ACTIVE_GAME_MSG, NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.GAME_MANAGER, String.format(GAME_STOP_ATTEMPT_LOG, player.getName()));
            return;
        }

        // Cancel the movement task for this player
        BukkitRunnable movementTask = movementTasks.get(player);
        if (movementTask != null) {
            movementTask.cancel();
        }
        movementTasks.remove(player);

        // Cancel the apple collection task for this player
        BukkitRunnable appleCollectionTask = appleCollectionTasks.get(player);
        if (appleCollectionTask != null) {
            appleCollectionTask.cancel();
        }
        appleCollectionTasks.remove(player);

        // Removing player snake
        snakeLifecycle.removeSnakeForPlayer(player);

        // Stop monitoring player inputs
        playerInputListener.removePlayer(player);

        // Stop scoring for player
        scoreManager.stopScore(player);

        // Retrieving lobby location and teleporting player
        Map<String, Object> gameData = activeGames.get(player.getUniqueId());
        Location lobbyLocation = (Location) gameData.get("lobbyLocation");
        player.teleport(lobbyLocation);

        // Remove the apple
        appleLifecycle.removeAppleForPlayer(player);

        // Removing player from the active games list
        activeGames.remove(player.getUniqueId());

        // Play the game end sound
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Informing the player that their game has been stopped
        player.sendMessage(Component.text(GAME_STOP_MSG, NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.GAME_MANAGER, String.format(GAME_STOPPED_LOG, player.getName()));
    }

    public void stopAllGames() {
        for (UUID playerId : activeGames.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                DebugManager.log(DebugManager.Category.GAME_MANAGER, String.format(GAME_STOP_ON_SHUTDOWN_LOG, player.getName()));
                stopGame(player);
            }
        }
    }
}