package com.slimer.Game;

import com.slimer.Util.DebugManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/**
 * Manages the player queues for different game modes, ensuring players are matched appropriately.
 * This class also listens for player events to manage queue status, such as removing a player from the
 * queue when they disconnect.
 *
 * <p>As a singleton, only one instance of this class will exist during the runtime. This ensures consistent
 * and centralized management of all player queues.</p>
 */
public class QueueManager implements Listener {

    private static QueueManager instance;
    private final Map<Location, List<Player>> pvpQueues = new HashMap<>();
    private final GameManager gameManager;
    private final SpawnLocationFinder spawnLocationFinder = new SpawnLocationFinder();
    private final Map<UUID, UUID> pvpAssociations = new HashMap<>();

    /**
     * Private constructor to ensure only one instance of QueueManager is created.
     */
    private QueueManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Returns the singleton instance of QueueManager, creating it if necessary.
     * This method should be used only for the first-time initialization.
     *
     * @return The singleton instance of QueueManager.
     */
    public static QueueManager getInstance(GameManager gameManager) {
        if (instance == null) {
            instance = new QueueManager(gameManager);
        }
        return instance;
    }

    /**
     * Returns the already initialized singleton instance of QueueManager.
     * This method should be used after the first-time initialization.
     *
     * @return The singleton instance of QueueManager.
     */
    public static QueueManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("QueueManager has not been initialized yet. Call getInstance(GameManager) first.");
        }
        return instance;
    }

    /**
     * Handles the queue mechanism for the PvP game mode. When a player invokes this method,
     * they are added to a queue specific to their current lobby location. Once the queue has
     * a sufficient number of players (2 for PvP), the game starts for the matched players.
     *
     * @param player        The player requesting to join the PvP queue.
     * @param lobbyLocation The location of the lobby from which the player is trying to start the game.
     * @param gameLocation  The location where the game will take place.
     * @return True if the player has been added to the queue or the game has started, false if the player is already in the queue.
     */
    public boolean handlePvPQueue(Player player, Location lobbyLocation, Location gameLocation, String regionName, World gameWorld) {
        // Check if the player is already in a PvP queue
        for (List<Player> queue : pvpQueues.values()) {
            if (queue.contains(player)) {
                player.sendMessage(Component.text("You are already in a PvP queue!", NamedTextColor.RED));
                DebugManager.log(DebugManager.Category.QUEUE_MANAGER, "Player: " + player.getName() + " is already in the PvP queue.");
                return false;
            }
        }

        List<Player> specificPvpQueue = pvpQueues.getOrDefault(lobbyLocation, new ArrayList<>());

        specificPvpQueue.add(player);
        DebugManager.log(DebugManager.Category.QUEUE_MANAGER, "Player: " + player.getName() + " added to the PvP queue.");

        Component commandComponent = Component.text("/snakegame stop", NamedTextColor.YELLOW)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.runCommand("/snakegame stop"));

        player.sendMessage(Component.text("You've been added to the PvP queue. Waiting for another player. To leave the queue, run ", NamedTextColor.YELLOW)
                .append(commandComponent)
                .append(Component.text(" or click on the command.", NamedTextColor.YELLOW)));

        pvpQueues.put(lobbyLocation, specificPvpQueue);

        if (specificPvpQueue.size() >= 2) {
            Player player1 = specificPvpQueue.remove(0);
            Player player2 = specificPvpQueue.remove(0);

            // Get spawn locations for both players
            Pair<Location, Location> validSpawnPoints = spawnLocationFinder.getValidPlayerSpawnPoints(gameWorld, gameLocation.getBlockY(), regionName);

            // Ensure valid spawn points were found
            if (validSpawnPoints.getLeft() == null || validSpawnPoints.getRight() == null) {
                player1.sendMessage(Component.text("Unable to find suitable spawn points. Please try again.", NamedTextColor.RED));
                player2.sendMessage(Component.text("Unable to find suitable spawn points. Please try again.", NamedTextColor.RED));
                return false;
            }

            // Add the players to the PvP associations map
            pvpAssociations.put(player1.getUniqueId(), player2.getUniqueId());
            pvpAssociations.put(player2.getUniqueId(), player1.getUniqueId());

            gameManager.startGame(player1, validSpawnPoints.getLeft(), lobbyLocation, "pvp");
            gameManager.startGame(player2, validSpawnPoints.getRight(), lobbyLocation, "pvp");
            player1.sendMessage(Component.text("PvP game starting!", NamedTextColor.GREEN));
            player2.sendMessage(Component.text("PvP game starting!", NamedTextColor.GREEN));
            DebugManager.log(DebugManager.Category.QUEUE_MANAGER, "PvP game starting for players: " + player1.getName() + " and " + player2.getName());
            return true;
        }
        return true;
    }

    /**
     * Checks if the provided player is in any PvP queue.
     *
     * @param player The player to check.
     * @return True if the player is in a PvP queue, false otherwise.
     */
    public boolean isPlayerInPvPQueue(Player player) {
        for (List<Player> queue : pvpQueues.values()) {
            if (queue.contains(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the UUID of the opponent associated with the provided player in a PvP match.
     * Returns null if the player is not in a PvP match or if an opponent is not found.
     *
     * @param playerUUID The UUID of the player whose opponent's UUID is to be fetched.
     * @return The UUID of the opponent in the PvP match, or null if not found.
     */
    public UUID getPvPAssociation(UUID playerUUID) {
        return pvpAssociations.get(playerUUID);
    }

    /**
     * Removes the PvP association between two players.
     * This method ensures that both players are no longer associated in the PvP match.
     *
     * @param player1UUID The UUID of the first player.
     * @param player2UUID The UUID of the second player.
     */
    public void removePvPAssociation(UUID player1UUID, UUID player2UUID) {
        pvpAssociations.remove(player1UUID);
        pvpAssociations.remove(player2UUID);
    }

    /**
     * Handles the event when a player quits the game.
     *
     * @param event The PlayerQuitEvent triggered when a player quits.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removePlayerFromQueues(player);
    }

    /**
     * Removes a player from all PvP queues.
     *
     * @param player The player to be removed from the PvP queues.
     */
    public void removePlayerFromQueues(Player player) {
        for (List<Player> queue : pvpQueues.values()) {
            if (queue.remove(player)) {
                DebugManager.log(DebugManager.Category.QUEUE_MANAGER, "Player: " + player.getName() + " removed from the PvP queue.");
            }
        }
    }
}