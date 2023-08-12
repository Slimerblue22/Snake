package com.slimer;

import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.playmode.MonoStereoMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MusicManager {
    private final Map<Player, RadioSongPlayer> songPlayers = new HashMap<>();

    // Starts the music for a specific player by loading and playing a song file
    public void startMusic(Player player, String pathToSongFile) {
        File songFile = new File(pathToSongFile);
        if (!songFile.exists()) {
            player.sendMessage(Component.text("Music file not found! Please ensure the song file exists at the specified path.", NamedTextColor.RED));
            return;
        }
        Song song = NBSDecoder.parse(songFile); // Decode the song file
        if (song == null) {
            player.sendMessage(Component.text("Error loading the music file. Please ensure it's a valid NBS file.", NamedTextColor.RED));
            return;
        }
        RadioSongPlayer songPlayer = new RadioSongPlayer(song); // Create a radio song player
        songPlayer.setChannelMode(new MonoStereoMode());
        songPlayer.addPlayer(player);
        songPlayer.setPlaying(true);
        songPlayer.setRepeatMode(RepeatMode.ONE); // Set to repeat mode
        songPlayers.put(player, songPlayer); // Store the player's song player
    }

    // Stops the music for a specific player
    public void stopMusic(Player player) {
        RadioSongPlayer songPlayer = songPlayers.get(player);
        if (songPlayer != null) {
            songPlayer.setPlaying(false); // Stop playing
            songPlayer.destroy(); // Destroy the song player
            songPlayers.remove(player); // Remove from active song players
        }
    }
}
