package com.slimer.Util;

import com.slimer.Main.Main;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.playmode.MonoStereoMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the music playback for the Snake game using the NoteBlockAPI.
 * This class is responsible for starting and stopping music for each player.
 */
public class MusicManager {
    private final Map<Player, RadioSongPlayer> songPlayers = new HashMap<>();
    private final Main mainPlugin;

    /**
     * Initializes a new instance of the MusicManager class.
     *
     * @param mainPlugin The main plugin instance.
     */
    public MusicManager(Main mainPlugin) {
        this.mainPlugin = mainPlugin;
    }

    /**
     * Starts playing music for the specified player.
     * If the song file is not found or cannot be played, logs an error message.
     *
     * @param player The player for whom to start the music.
     */
    public void startMusic(Player player) {
        // Get the song file path from the configuration
        String pathToSongFile = mainPlugin.getSongFilePath();

        // Generate the full path to the song file
        String fullPath = Paths.get(mainPlugin.getDataFolder().getAbsolutePath(), pathToSongFile).toString();
        File songFile = new File(fullPath);

        // Create song folder if it doesn't exist
        File songFolder = songFile.getParentFile();
        if (!songFolder.exists()) {
            if (!songFolder.mkdirs()) {
                Bukkit.getLogger().severe("[MusicManager.java] Failed to create songs folder. It may already exist.");
            }
        }

        // Check if song file exists
        if (!songFile.exists()) {
            Bukkit.getLogger().warning("[MusicManager.java] No song file found at " + fullPath + ". Music will not be played.");
            return;
        }

        // Try to start the music
        try {
            Song song = NBSDecoder.parse(songFile);
            if (song == null) {
                Bukkit.getLogger().severe("[MusicManager.java] Error loading the music file. Please ensure it's a valid NBS file.");
                return;
            }

            RadioSongPlayer songPlayer = new RadioSongPlayer(song);
            songPlayer.setChannelMode(new MonoStereoMode());
            songPlayer.addPlayer(player);
            songPlayer.setPlaying(true);
            songPlayer.setRepeatMode(RepeatMode.ONE);
            songPlayers.put(player, songPlayer);

        } catch (Exception e) {
            Bukkit.getLogger().severe("[MusicManager.java] Could not load song from file " + fullPath + ": " + e.getMessage());
        }
    }

    /**
     * Stops the music for the specified player.
     *
     * @param player The player for whom to stop the music.
     */
    public void stopMusic(Player player) {
        RadioSongPlayer songPlayer = songPlayers.get(player);
        if (songPlayer != null) {
            songPlayer.setPlaying(false);
            songPlayer.destroy();
            songPlayers.remove(player);
        }
    }
}
