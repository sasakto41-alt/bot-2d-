package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.logging.Level;

/**
 * Spawns floating holograms (invisible ArmorStands with visible custom
 * names) on top of the fence around each mine's platform. The player can
 * right-click a hologram to trigger one of four actions:
 *
 *   MENU          — open the main Deep Digger GUI
 *   UPGRADE       — open the "upgrade mine" GUI
 *   PICKAXE       — open the pickaxe shop
 *   REFRESH_MINE  — instantly regenerate every block in the shaft
 *
 * Holograms are tagged via PersistentDataContainer so they can be safely
 * detected, removed and respawned. On plugin enable we respawn fresh
 * holograms for every loaded mine.
 */
public class HologramManager {

    public static final String PREFIX = "dd_holo_";

    public enum Action { MENU, UPGRADE, PICKAXE, REFRESH_MINE }

    private final DeepDiggerPlugin plugin;

    public HologramManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Removes any existing holograms for the mine and spawns four fresh
     * ones on the four sides of the platform fence.
     */
    public void spawnFor(MineData md) {
        if (md == null) return;
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        if (w == null) return;
        // Kill old holograms first.
        removeAllFor(md);

        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int top = md.getSurfaceY();
        int h = md.halfWidth();
        int platformRadius = h + 1; // fence is one block out from the platform edge

        // Place holograms INSIDE the fence ring, at the four cardinal sides
        // of the platform. They float just above the platform surface so the
        // player can stand on the platform and right-click them easily.
        double y = top + 1.2;
        spawnOne(w, cx + platformRadius, y, cz,                       md, Action.MENU,         "\u00A76\u00A7l\u26CF МЕНЮ");
        spawnOne(w, cx - platformRadius, y, cz,                       md, Action.UPGRADE,      "\u00A76\u00A7l\u2B06 УЛУЧШИТЬ");
        spawnOne(w, cx,                y, cz + platformRadius,        md, Action.PICKAXE,      "\u00A76\u00A7l\u26CF КИРКИ");
        spawnOne(w, cx,                y, cz - platformRadius,        md, Action.REFRESH_MINE, "\u00A76\u00A7l\u21BB ОБНОВИТЬ ШАХТУ");
    }

    private void spawnOne(World w, int x, double y, int z, MineData md, Action action, String display) {
        try {
            Location loc = new Location(w, x + 0.5, y, z + 0.5);
            Entity e = w.spawnEntity(loc, EntityType.ARMOR_STAND);
            if (!(e instanceof ArmorStand)) return;
            ArmorStand as = (ArmorStand) e;
            as.setVisible(false);
            as.setCustomName(display);
            as.setCustomNameVisible(true);
            as.setGravity(false);
            as.setMarker(true);
            as.setInvulnerable(true);
            as.setCollidable(false);
            as.setSmall(true);
            as.setAI(false);
            as.setSilent(true);
            as.setPersistent(true); // keep across chunk unloads
            // Disable slot interaction so players can't put items on it.
            as.addDisabledSlots(org.bukkit.inventory.EquipmentSlot.HEAD,
                    org.bukkit.inventory.EquipmentSlot.CHEST,
                    org.bukkit.inventory.EquipmentSlot.LEGS,
                    org.bukkit.inventory.EquipmentSlot.FEET,
                    org.bukkit.inventory.EquipmentSlot.HAND,
                    org.bukkit.inventory.EquipmentSlot.OFF_HAND);
            as.getPersistentDataContainer().set(plugin.getHoloActionKey(),
                    PersistentDataType.STRING, action.name());
            as.getPersistentDataContainer().set(plugin.getHoloMineKey(),
                    PersistentDataType.STRING, md.getKey());
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to spawn hologram at " + x + "," + y + "," + z + " for " + md.getKey(), t);
        }
    }

    /**
     * Returns the action a clicked ArmorStand represents, or null if it is
     * not one of our holograms.
     */
    public Action actionFor(Entity entity) {
        if (!(entity instanceof ArmorStand)) return null;
        ArmorStand as = (ArmorStand) entity;
        String stored = as.getPersistentDataContainer().get(plugin.getHoloActionKey(),
                PersistentDataType.STRING);
        if (stored == null) return null;
        try { return Action.valueOf(stored); } catch (IllegalArgumentException e) { return null; }
    }

    public String mineKeyFor(Entity entity) {
        if (!(entity instanceof ArmorStand)) return null;
        ArmorStand as = (ArmorStand) entity;
        String stored = as.getPersistentDataContainer().get(plugin.getHoloMineKey(),
                PersistentDataType.STRING);
        return stored;
    }

    /**
     * Removes every hologram belonging to the given mine by scanning the
     * chunks around the mine's surface position.
     */
    public void removeAllFor(MineData md) {
        if (md == null) return;
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        if (w == null) return;
        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int h = md.halfWidth();
        int platformRadius = h + 2;
        for (int dx = -platformRadius; dx <= platformRadius; dx++) {
            for (int dz = -platformRadius; dz <= platformRadius; dz++) {
                int chunkX = (cx + dx) >> 4;
                int chunkZ = (cz + dz) >> 4;
                if (!w.isChunkLoaded(chunkX, chunkZ)) continue;
                Chunk chunk = w.getChunkAt(chunkX, chunkZ, false);
                if (chunk == null) continue;
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof ArmorStand)) continue;
                    String key = mineKeyFor(e);
                    if (key != null && key.equals(md.getKey())) {
                        e.remove();
                    }
                }
            }
        }
    }
}
