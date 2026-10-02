package com.example.deepdigger.tasks;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Applies darkness/blindness to players in a shaft with no nearby torch.
 */
public class DarknessTask extends BukkitRunnable {

    private final DeepDiggerPlugin plugin;
    private PotionEffectType darknessType = null;
    private PotionEffectType fallbackType = null;

    public DarknessTask(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
        // Resolve DARKNESS if available, else BLINDNESS.
        try {
            darknessType = org.bukkit.potion.PotionEffectType.getByName("DARKNESS");
        } catch (Throwable ignored) {}
        try {
            fallbackType = org.bukkit.potion.PotionEffectType.getByName("BLINDNESS");
        } catch (Throwable ignored) {}
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().darknessEnabled()) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            try {
                process(p);
            } catch (Throwable t) {
                plugin.getLogger().warning("Darkness task failed for " + p.getName()
                        + ": " + t.getMessage());
            }
        }
    }

    private void process(Player p) {
        Location loc = p.getLocation();
        MineData md = plugin.getMineManager().getMineAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (md == null) {
            // Outside a mine — clear darkness if we previously applied it.
            clearDarkness(p);
            return;
        }
        // Only apply if inside the shaft (not on the surface).
        if (!md.isInsideShaft(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
            clearDarkness(p);
            return;
        }
        // Look for torches in radius.
        int radius = plugin.getConfigManager().torchRadius();
        boolean nearTorch = scanForTorch(loc, radius);
        if (nearTorch) {
            clearDarkness(p);
            return;
        }
        // Apply darkness.
        PotionEffectType t = pickType();
        if (t == null) return;
        if (p.hasPotionEffect(t)) return;
        p.addPotionEffect(new PotionEffect(t, plugin.getConfigManager().darknessCheckPeriod() + 20,
                0, false, false, false));
    }

    private PotionEffectType pickType() {
        if (darknessType != null) return darknessType;
        if (plugin.getConfigManager().darknessFallbackBlindness() && fallbackType != null) {
            return fallbackType;
        }
        return null;
    }

    private void clearDarkness(Player p) {
        if (darknessType != null) p.removePotionEffect(darknessType);
        if (fallbackType != null) p.removePotionEffect(fallbackType);
    }

    /**
     * Searches blocks around the player for torches. Uses Material names
     * for forward compatibility with newer Paper versions where torches
     * might be split into WALL_TORCH and TORCH.
     */
    private boolean scanForTorch(Location center, int radius) {
        int px = center.getBlockX();
        int py = center.getBlockY();
        int pz = center.getBlockZ();
        org.bukkit.World w = center.getWorld();
        if (w == null) return false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = w.getBlockAt(px + dx, py + dy, pz + dz);
                    if (b.getType() == Material.TORCH || b.getType() == Material.WALL_TORCH) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
