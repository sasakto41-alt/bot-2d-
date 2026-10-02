package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerRespawnListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        // Send the player back to the surface of their own mine.
        MineData md = plugin.getMineManager().getMineByOwner(e.getPlayer().getUniqueId());
        if (md != null) {
            Location loc = plugin.getMineManager().surfaceLocation(md);
            e.setRespawnLocation(loc);
        }
    }
}
