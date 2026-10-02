package com.example.deepdigger.tasks;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Heals players standing on their own mine's platform. Every second, any
 * online player whose feet are at the platform Y of their own mine (or
 * the mine they're working in) and within the fence ring gets +1 HP
 * up to their max health.
 */
public class HealTask extends BukkitRunnable {

    private final DeepDiggerPlugin plugin;

    public HealTask(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            try {
                healIfOnPlatform(p);
            } catch (Throwable t) {
                // ignore — keep going for other players
            }
        }
    }

    private void healIfOnPlatform(Player p) {
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        if (md == null) {
            com.example.deepdigger.models.PlayerData pd =
                    plugin.getMineManager().get(p.getUniqueId());
            if (pd != null && pd.getWorkingForMine() != null) {
                md = plugin.getMineManager().getMine(pd.getWorkingForMine());
            }
        }
        if (md == null) return;
        Location loc = p.getLocation();
        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int h = md.halfWidth();
        int fenceRadius = h + 2;
        int top = md.getSurfaceY();
        // Player must be on the platform (feet at top, within fence ring).
        int dx = loc.getBlockX() - cx;
        int dz = loc.getBlockZ() - cz;
        if (Math.max(Math.abs(dx), Math.abs(dz)) > fenceRadius) return;
        // Allow healing on the platform top (y == top) and the air above it
        // (y == top + 1, top + 2 for jumps). Don't heal inside the shaft.
        int y = loc.getBlockY();
        if (y < top) return;
        // Heal +1 up to max.
        double max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        if (p.getHealth() >= max - 0.01) return;
        double newHealth = Math.min(max, p.getHealth() + 1.0);
        p.setHealth(newHealth);
    }
}
