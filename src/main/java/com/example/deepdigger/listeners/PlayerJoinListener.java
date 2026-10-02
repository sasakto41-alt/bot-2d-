package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
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
        // Auto-create or auto-recover the player's mine.
        MineData md = plugin.getMineManager().getMineByOwner(e.getPlayer().getUniqueId());
        if (md == null) {
            // No mine yet — create one and teleport the player to it.
            md = plugin.getMineManager().createMineFor(e.getPlayer().getUniqueId(), e.getPlayer().getName());
            plugin.getMessageManager().send(e.getPlayer(), "mine-created");
        } else if (md.getShaftWidth() < 3) {
            // Old-format mine (1x1) — recreate with the new 3x3 layout.
            plugin.getMineManager().deleteMine(e.getPlayer().getUniqueId());
            md = plugin.getMineManager().createMineFor(e.getPlayer().getUniqueId(), e.getPlayer().getName());
            plugin.getMessageManager().sendRaw(e.getPlayer(),
                    plugin.getMessageManager().raw("prefix")
                            + "&eСтарая шахта пересоздана в новом формате 3x3.");
        } else {
            // Existing valid mine — make sure holograms are present.
            if (plugin.getHologramManager() != null) {
                plugin.getHologramManager().spawnFor(md);
            }
        }
        // Ensure player data is loaded.
        plugin.getMineManager().getOrCreate(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        // Teleport the player to their mine on every join (per user request).
        if (md != null) {
            org.bukkit.Location loc = plugin.getMineManager().surfaceLocation(md);
            if (loc != null && loc.getWorld() != null) {
                // Delay teleport by one tick so joining player state is ready.
                final MineData finalMd = md;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    e.getPlayer().teleport(plugin.getMineManager().surfaceLocation(finalMd));
                }, 1L);
            }
        }
        plugin.getHudManager().apply(e.getPlayer());
    }
}
