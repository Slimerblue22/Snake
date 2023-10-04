package com.slimer.Game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public boolean handlePvPQueue(Player player, Location lobbyLocation, Location gameLocation) {
        // Check if the player is already in a PvP queue
        for (List<Player> queue : pvpQueues.values()) {
            if (queue.contains(player)) {
                player.sendMessage(Component.text("You are already in a PvP queue!", NamedTextColor.RED));
                return false;
            }
        }

        List<Player> specificPvpQueue = pvpQueues.getOrDefault(lobbyLocation, new ArrayList<>());

        specificPvpQueue.add(player);
        player.sendMessage(Component.text("You've been added to the PvP queue. Waiting for another player...", NamedTextColor.YELLOW));
        pvpQueues.put(lobbyLocation, specificPvpQueue);

        if (specificPvpQueue.size() >= 2) {
            Player player1 = specificPvpQueue.remove(0);
            Player player2 = specificPvpQueue.remove(0);
            gameManager.startGame(player1, gameLocation, lobbyLocation, "pvp");
            gameManager.startGame(player2, gameLocation, lobbyLocation, "pvp");
            player1.sendMessage(Component.text("PvP game starting!", NamedTextColor.GREEN));
            player2.sendMessage(Component.text("PvP game starting!", NamedTextColor.GREEN));
            return true;
        }
        return true;
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
            queue.remove(player);
        }
    }
}