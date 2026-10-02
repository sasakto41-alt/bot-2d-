package com.example.deepdigger.tasks;

import com.example.deepdigger.DeepDiggerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodically refreshes the sidebar for online players.
 */
public class HudUpdateTask extends BukkitRunnable {

    private final DeepDiggerPlugin plugin;

    public HudUpdateTask(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            try {
                plugin.getHudManager().apply(p);
            } catch (Throwable t) {
                // Continue for other players.
                plugin.getLogger().warning("HUD update failed for " + p.getName()
                        + ": " + t.getMessage());
            }
        }
    }
}
