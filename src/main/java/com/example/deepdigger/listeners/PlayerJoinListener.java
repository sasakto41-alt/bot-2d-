package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerJoinListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Create mine for player if missing.
        if (plugin.getMineManager().getMineByOwner(e.getPlayer().getUniqueId()) == null) {
            plugin.getMineManager().createMineFor(e.getPlayer().getUniqueId(), e.getPlayer().getName());
            plugin.getMessageManager().send(e.getPlayer(), "mine-created");
        }
        // Ensure player data is loaded.
        plugin.getMineManager().getOrCreate(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        plugin.getHudManager().apply(e.getPlayer());
    }
}
