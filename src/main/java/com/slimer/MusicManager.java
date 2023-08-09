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
    // This method starts playing the song specified by the path for the given player.
    // It creates a new RadioSongPlayer, sets the mode, adds the player, and starts the song.
    public void startMusic(Player player, String pathToSongFile) {
        File songFile = new File(pathToSongFile);

        if (!songFile.exists()) {
            player.sendMessage(Component.text("Music file not found! Please ensure the song file exists at the specified path.", NamedTextColor.RED));
            return;
        }

        Song song = NBSDecoder.parse(songFile);

        if (song == null) {
            player.sendMessage(Component.text("Error loading the music file. Please ensure it's a valid NBS file.", NamedTextColor.RED));
            return;
        }

        RadioSongPlayer songPlayer = new RadioSongPlayer(song);
        songPlayer.setChannelMode(new MonoStereoMode());
        songPlayer.addPlayer(player);
        songPlayer.setPlaying(true);
        songPlayer.setRepeatMode(RepeatMode.ONE);
        songPlayers.put(player, songPlayer);
    }
    // This method stops the music for the given player.
    // It retrieves the RadioSongPlayer for the player, stops the song, destroys the player, and removes the player from the map.
    public void stopMusic(Player player) {
        RadioSongPlayer songPlayer = songPlayers.get(player);
        if (songPlayer != null) {
            songPlayer.setPlaying(false);
            songPlayer.destroy();
            songPlayers.remove(player);
        }
    }
}
