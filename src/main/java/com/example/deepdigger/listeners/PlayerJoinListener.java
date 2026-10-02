package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PlayerData;
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
        PlayerData pd = plugin.getMineManager().getOrCreate(e.getPlayer().getUniqueId(), e.getPlayer().getName());

        // If the player is a worker somewhere, send them to the owner's mine
        // so they can immediately start digging.
        if (pd != null && pd.getWorkingForMine() != null) {
            MineData ownerMine = plugin.getMineManager().getMine(pd.getWorkingForMine());
            if (ownerMine != null) {
                // Make sure holograms are spawned for the owner's mine.
                if (plugin.getHologramManager() != null) {
                    plugin.getHologramManager().spawnFor(ownerMine);
                }
                final MineData finalOwnerMine = ownerMine;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    e.getPlayer().teleport(plugin.getMineManager().surfaceLocation(finalOwnerMine));
                }, 1L);
                plugin.getHudManager().apply(e.getPlayer());
                return;
            }
            // Owner's mine missing — fall through and create the player's own.
        }

        // Auto-create or auto-recover the player's own mine.
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
        // Teleport the player to their mine on every join (per user request).
        if (md != null) {
            final MineData finalMd = md;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                e.getPlayer().teleport(plugin.getMineManager().surfaceLocation(finalMd));
            }, 1L);
        }
        plugin.getHudManager().apply(e.getPlayer());
    }
}
