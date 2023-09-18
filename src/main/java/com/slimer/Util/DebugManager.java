package com.slimer.Util;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class for managing debug mode in the application.
 * Provides a global toggle to enable or disable debug messages.
 */
public class DebugManager {

    /**
     * A boolean flag indicating if debug mode is enabled or disabled.
     */
    public static boolean isDebugEnabled = false;

    /**
     * Toggles the debug mode.
     * If debug mode is enabled, it will be disabled, and vice versa.
     */
    public static void toggleDebug() {
        isDebugEnabled = !isDebugEnabled;
    }

    /**
     * Generates a formatted debug message prefixed with the plugin's version.
     *
     * @param message The specific debug message to be logged.
     * @return A formatted string with the plugin's version and the provided message.
     */
    public static String getDebugMessage(String message) {
        String version = "2.0.1";
        return "{Snake " + version + " DEBUG} " + message;
    }

    /**
     * A nested class that implements CommandExecutor to toggle debug mode via commands.
     */
    public static class ToggleDebugCommand implements CommandExecutor {

        /**
         * Executes the given command, returning its success.
         *
         * @param sender  Source of the command.
         * @param command Command which was executed.
         * @param label   Alias of the command which was used.
         * @param args    Passed command arguments.
         * @return A boolean indicating the success of the command.
         */
        @Override
        public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            toggleDebug();
            sender.sendMessage("Debug mode " + (isDebugEnabled ? "enabled" : "disabled"));
            return true;
        }
    }
}
