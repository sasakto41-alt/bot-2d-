package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Picks an ore to drop for a freshly mined block based on depth.
 */
public class OreManager {

    private final DeepDiggerPlugin plugin;

    public OreManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns reward + ore name + block material to drop for a freshly mined
     * shaft block at the given depth. Result is a 3-entry array:
     * 0: Material (block drop material, e.g. COAL_ORE -> COAL)
     * 1: double reward
     * 2: String display name (e.g. "уголь")
     */
    public Object[] rollForDepth(int depth) {
        List<Map<String, Object>> list = plugin.getConfigManager().oreList();
        Map<String, Object> chosen = null;
        for (Map<String, Object> ore : list) {
            int min = (int) ore.get("min-depth");
            int max = (int) ore.get("max-depth");
            if (depth < min || depth > max) continue;
            double chance = (double) ore.get("chance");
            if (chance >= 1.0) {
                if (chosen == null) chosen = ore; // fallback
            } else if (Math.random() < chance) {
                chosen = ore;
                break;
            }
        }
        if (chosen == null) {
            // No ore rolled, just plain stone.
            return new Object[]{ Material.STONE, 1.0, "камень" };
        }
        String name = (String) chosen.get("name");
        String block = (String) chosen.get("block");
        Material blockMat = Material.matchMaterial(block);
        if (blockMat == null) blockMat = Material.STONE;
        double reward = (double) chosen.get("reward");
        String displayName;
        switch (name.toLowerCase()) {
            case "coal": displayName = "уголь"; break;
            case "iron": displayName = "железо"; break;
            case "gold": displayName = "золото"; break;
            case "diamond": displayName = "алмаз"; break;
            case "stone": displayName = "камень"; break;
            default: displayName = name; break;
        }
        return new Object[]{ blockMat, reward, displayName };
    }

    public boolean isRare(Material blockMat) {
        return blockMat == Material.DIAMOND_ORE
                || blockMat == Material.GOLD_ORE;
    }
}
