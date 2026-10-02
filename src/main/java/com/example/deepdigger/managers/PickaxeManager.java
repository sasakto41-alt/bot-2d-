package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.PickaxeType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds and inspects Deep Digger pickaxes. A pickaxe item is recognized
 * by its PersistentDataContainer key "deepdigger.pickaxe".
 */
public class PickaxeManager {

    public static final String NAMESPACE = "deepdigger";
    public static final String KEY_PICKAXE = "pickaxe";
    public static final String KEY_DURABILITY = "durability";
    public static final String KEY_TIER = "tier";

    private final DeepDiggerPlugin plugin;
    private NamespacedKey pickaxeKey;
    private NamespacedKey durabilityKey;
    private NamespacedKey tierKey;

    public PickaxeManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        pickaxeKey = new NamespacedKey(plugin, KEY_PICKAXE);
        durabilityKey = new NamespacedKey(plugin, KEY_DURABILITY);
        tierKey = new NamespacedKey(plugin, KEY_TIER);
    }

    public NamespacedKey pickaxeKey() { return pickaxeKey; }
    public NamespacedKey durabilityKey() { return durabilityKey; }
    public NamespacedKey tierKey() { return tierKey; }

    public ItemStack buildPickaxe(PickaxeType type) {
        Map<String, Map<String, Object>> catalog = plugin.getConfigManager().pickaxeCatalog();
        Map<String, Object> entry = catalog.get(type.name());
        if (entry == null) {
            // Fallback to vanilla wooden pickaxe.
            return new ItemStack(Material.WOODEN_PICKAXE);
        }
        String matName = (String) entry.get("material");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.WOODEN_PICKAXE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String display = plugin.getConfigManager().msg("prefix"); // ignore, use entry
            display = (String) entry.get("display");
            meta.setDisplayName(com.example.deepdigger.managers.MessageManager.color(display));
            List<String> lore = new ArrayList<>();
            List<String> rawLore = (List<String>) entry.get("lore");
            if (rawLore != null) {
                for (String l : rawLore) {
                    lore.add(com.example.deepdigger.managers.MessageManager.color(l));
                }
            }
            meta.setLore(lore);
            meta.setUnbreakable(true);
            int durability = (int) entry.get("durability");
            if (meta instanceof Damageable) {
                ((Damageable) meta).setDamage(0);
            }
            meta.getPersistentDataContainer().set(pickaxeKey, PersistentDataType.STRING, type.name());
            meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, type.tier());
            meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, durability);
            applyEnchantments(meta, type);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Adds enchantments to the pickaxe based on its tier:
     *   STONE   — Silk Touch
     *   IRON    — Fortune I
     *   DIAMOND — Fortune III + Efficiency II
     *
     * Enchantments are applied with unsafe=true so we can bypass vanilla
     * level limits.
     */
    private void applyEnchantments(org.bukkit.inventory.meta.ItemMeta meta, PickaxeType type) {
        if (meta == null) return;
        switch (type) {
            case STONE:
                meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                break;
            case IRON:
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 1, true);
                break;
            case DIAMOND:
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 3, true);
                meta.addEnchant(Enchantment.DIG_SPEED, 2, true);
                break;
            default:
                break;
        }
    }

    public PickaxeType typeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String stored = meta.getPersistentDataContainer().get(pickaxeKey, PersistentDataType.STRING);
        if (stored == null) return null;
        try {
            return PickaxeType.valueOf(stored);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Returns the tier of the pickaxe the player currently holds in their main
     * hand. If not a Deep Digger pickaxe, falls back to the player's recorded
     * tier in their PlayerData.
     */
    public PickaxeType heldType(org.bukkit.entity.Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        PickaxeType t = typeOf(inHand);
        if (t != null) return t;
        com.example.deepdigger.models.PlayerData pd = plugin.getMineManager().get(player.getUniqueId());
        return pd == null ? PickaxeType.WOOD : pd.getPickaxe();
    }

    public int price(PickaxeType type) {
        Map<String, Map<String, Object>> catalog = plugin.getConfigManager().pickaxeCatalog();
        Map<String, Object> entry = catalog.get(type.name());
        if (entry == null) return 0;
        Object p = entry.get("price");
        if (p instanceof Number) return ((Number) p).intValue();
        return 0;
    }

    /**
     * Returns the required pickaxe tier for a given ore block.
     */
    public PickaxeType requiredPickaxe(Material ore) {
        for (Map<String, Object> e : plugin.getConfigManager().oreList()) {
            String block = (String) e.get("block");
            Material m = Material.matchMaterial(block);
            if (m != null && m == ore) {
                String req = (String) e.get("required-pickaxe");
                try {
                    return PickaxeType.valueOf(req);
                } catch (IllegalArgumentException ex) {
                    return PickaxeType.WOOD;
                }
            }
        }
        return PickaxeType.WOOD;
    }

    public boolean isAtLeast(PickaxeType held, PickaxeType required) {
        return held != null && required != null && held.tier() >= required.tier();
    }

    public int maxDurability(PickaxeType type) {
        Map<String, Map<String, Object>> catalog = plugin.getConfigManager().pickaxeCatalog();
        Map<String, Object> entry = catalog.get(type.name());
        if (entry == null) return 100;
        Object d = entry.get("durability");
        return d instanceof Number ? ((Number) d).intValue() : 100;
    }
}
