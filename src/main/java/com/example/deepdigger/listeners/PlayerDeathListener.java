package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerDeathListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        // Keep inventory & levels so progress is preserved.
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        e.getDrops().clear();
    }
}
