package com.slimer.Game;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// TODO: This is for debugging, remove it later!

public class IncrementScoreCommand implements CommandExecutor {
    private final ScoreManager scoreManager;

    public IncrementScoreCommand(ScoreManager scoreManager) {
        this.scoreManager = scoreManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Command format: /incscore <playerName>
        if (args.length != 1) {
            sender.sendMessage("Usage: /incscore <playerName>");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        scoreManager.updateScore(player);
        sender.sendMessage("Score incremented for " + player.getName());
        return true;
    }
}
