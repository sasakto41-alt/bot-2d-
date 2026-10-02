package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Regenerates blocks inside a mine shaft after they've been mined.
 * <p>
 * Each mined block is queued with a small delay; the block reappears as
 * either plain stone or ore depending on depth and chance.
 */
public class RegenManager {

    private final DeepDiggerPlugin plugin;
    private final Map<Location, Long> pending = new HashMap<>();
    private BukkitRunnable task;

    public RegenManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int delay = Math.max(1, plugin.getConfigManager().regenDelaySeconds()) * 20;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                processTick();
            }
        };
        task.runTaskTimer(plugin, delay, 10);
    }

    public void stop() {
        if (task != null) {
            try { task.cancel(); } catch (IllegalStateException ignored) {}
        }
    }

    public void scheduleRegen(Block block) {
        if (block == null) return;
        long fireAt = System.currentTimeMillis()
                + plugin.getConfigManager().regenDelaySeconds() * 1000L;
        pending.put(block.getLocation(), fireAt);
    }

    private void processTick() {
        if (pending.isEmpty()) return;
        long now = System.currentTimeMillis();
        // Avoid ConcurrentModificationException.
        Location[] keys = pending.keySet().toArray(new Location[0]);
        for (Location loc : keys) {
            Long fireAt = pending.get(loc);
            if (fireAt == null || fireAt > now) continue;
            pending.remove(loc);
            Block b = loc.getBlock();
            if (b.getType() != Material.AIR) continue;
            // Find which mine this belongs to.
            MineData md = plugin.getMineManager().getMineAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            if (md == null) continue;
            // Determine depth.
            int depth = md.depthAt(loc.getBlockY());
            if (depth < 0 || depth > md.getDepth()) continue;
            Material newMat = plugin.getMineManager().pickOreForDepth(depth);
            b.setType(newMat, false);
        }
    }

    public boolean isPending(Location loc) {
        return pending.containsKey(loc);
    }
}
