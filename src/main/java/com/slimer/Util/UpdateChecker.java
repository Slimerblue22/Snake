package com.slimer.Util;

import com.slimer.Main;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles checking for plugin updates from the GitHub repository.
 * This operation should only be initiated during startup.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class UpdateChecker {
    private Logger logger;

    /**
     * Checks for updates by querying the latest release from the GitHub repository.
     * Compares the current plugin version with the latest available version on GitHub
     * and logs information about the update status.
     *
     * @param main The main class instance for accessing shared resources.
     */
    public void checkForUpdates(Main main) {
        logger = main.getLogger();
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://api.github.com/repos/Slimerblue22/Snake/releases/latest");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logger.log(Level.WARNING, "[UpdateChecker] Update checker received non-OK response from GitHub: " + responseCode);
                    return;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = reader.readLine()) != null) {
                        response.append(inputLine);
                    }

                    // Remove the `V` in the GitHub version then compare
                    JSONParser parser = new JSONParser();
                    JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                    String latestVersion = ((String) jsonResponse.get("tag_name")).replaceFirst("^V", "");

                    // TODO: Used during testing, remove before release
                    String version = Main.getPluginVersion();
                    logger.log(Level.INFO, "[UpdateChecker] Current plugin version: " + version);
                    logger.log(Level.INFO, "[UpdateChecker] Latest available version: " + latestVersion);

                    if (!version.equalsIgnoreCase(latestVersion)) {
                        logger.log(Level.INFO, "[UpdateChecker] An update is available for the plugin. Current: " + version + " Latest: " + latestVersion);
                    } else {
                        logger.log(Level.INFO, "[UpdateChecker] Plugin is up to date.");
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[UpdateChecker] An error occurred while checking for updates: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}