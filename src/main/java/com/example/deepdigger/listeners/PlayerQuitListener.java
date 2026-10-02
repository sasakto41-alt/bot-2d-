package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerQuitListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getMineManager().saveAll();
        plugin.getHudManager().clear(e.getPlayer());
    }
}
