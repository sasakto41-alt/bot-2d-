package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.managers.MessageManager;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PickaxeType;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

/**
 * Handles actual mining inside the personal shaft.
 */
public class BlockBreakListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public BlockBreakListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();
        MineData md = plugin.getMineManager().getMineAt(b.getX(), b.getY(), b.getZ());
        if (md == null) {
            return; // Block is outside any mine — let Bukkit handle it normally.
        }
        // Block is part of a mine. Walls (glass) and bottom (bedrock) can never be broken.
        if (md.isWall(b.getX(), b.getY(), b.getZ())) {
            e.setCancelled(true);
            plugin.getMessageManager().send(p, "cannot-break-wall");
            return;
        }
        if (md.isBottom(b.getX(), b.getY(), b.getZ())) {
            e.setCancelled(true);
            plugin.getMessageManager().send(p, "cannot-break-wall");
            return;
        }
        if (md.isSafeZone(b.getX(), b.getY(), b.getZ())) {
            e.setCancelled(true);
            plugin.getMessageManager().send(p, "cannot-break-wall");
            return;
        }
        // Block must be inside the shaft.
        if (!md.isInsideShaft(b.getX(), b.getY(), b.getZ())) {
            e.setCancelled(true);
            return;
        }
        // Access check.
        if (!plugin.getMineManager().hasAccess(p.getUniqueId(), md)) {
            e.setCancelled(true);
            plugin.getMessageManager().send(p, "not-allowed-mine");
            return;
        }
        // Cancel vanilla drop; we'll provide our own.
        e.setDropItems(false);
        e.setCancelled(true);

        // Determine depth and the block material currently broken.
        int depth = md.depthAt(b.getY());
        if (depth < 0 || depth > md.getDepth()) {
            return;
        }

        // Pickaxe check.
        Material currentBlock = b.getType();
        PickaxeType required = plugin.getPickaxeManager().requiredPickaxe(currentBlock);
        PickaxeType held = plugin.getPickaxeManager().heldType(p);
        if (!plugin.getPickaxeManager().isAtLeast(held, required)) {
            plugin.getMessageManager().send(p, "better-pickaxe");
            return;
        }

        // Roll reward.
        Object[] roll = plugin.getOreManager().rollForDepth(depth);
        Material dropMat = (Material) roll[0];
        double reward = (Double) roll[1];
        String displayName = (String) roll[2];

        // Damage pickaxe by 1 if it's a Deep Digger pickaxe.
        ItemStack inHand = p.getInventory().getItemInMainHand();
        PickaxeType heldType = plugin.getPickaxeManager().typeOf(inHand);
        if (heldType != null && inHand.getItemMeta() instanceof Damageable) {
            Damageable dmg = (Damageable) inHand.getItemMeta();
            int current = dmg.getDamage();
            int maxDmg = inHand.getType().getMaxDurability();
            if (current + 1 >= maxDmg) {
                // Break pickaxe.
                p.getInventory().setItemInMainHand(null);
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                // Revert to WOOD in player data.
                PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
                if (pd != null) pd.setPickaxe(PickaxeType.WOOD);
                // Auto-give the player a fresh wooden pickaxe so they can keep
                // playing. The starter pickaxe is intentionally weak so the
                // player still wants to upgrade.
                ItemStack fresh = plugin.getPickaxeManager().buildPickaxe(PickaxeType.WOOD);
                p.getInventory().addItem(fresh);
            } else {
                dmg.setDamage(current + 1);
                inHand.setItemMeta(dmg);
            }
        }

        // Money split.
        PlayerData pd = plugin.getMineManager().getOrCreate(p.getUniqueId(), p.getName());
        plugin.getEconomyManager().payMiningReward(pd, reward);

        // NOTE: no item drop. The user explicitly requested that breaking a
        // block does not produce any inventory loot — money is added
        // directly to the balance instead. This avoids inventory clutter
        // and keeps the gameplay loop focused on currency accumulation.

        // Particles + sound.
        if (plugin.getOreManager().isRare(currentBlock) || "алмаз".equals(displayName)
                || "золото".equals(displayName)) {
            p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfigManager().rareSound()), 1f, 1f);
            b.getWorld().spawnParticle(
                    org.bukkit.Particle.valueOf(plugin.getConfigManager().rareParticle()),
                    b.getLocation().clone().add(0.5, 0.5, 0.5),
                    18, 0.2, 0.2, 0.2, 0.05);
        } else {
            p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfigManager().commonSound()), 1f, 1f);
        }
        p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfigManager().moneySound()), 0.4f, 1.5f);

        // Messages.
        if ("камень".equals(displayName)) {
            plugin.getMessageManager().actionBar(p,
                    "&7⛏ " + plugin.getMessageManager().raw("mined-stone"));
        } else {
            plugin.getMessageManager().send(p, "mined-ore", "ore", displayName);
        }
        plugin.getMessageManager().actionBar(p,
                plugin.getConfigManager().msg("money-earned",
                        "amount", plugin.getEconomyManager().format(reward)));

        // Schedule regen.
        plugin.getRegenManager().scheduleRegen(b);

        // Set block to air visually.
        b.setType(Material.AIR, false);

        // Random events — 1% chance per mined block. Per user request,
        // we trigger one of two events:
        //   "Старая кирка" — pickaxe damage +20 (closer to breaking).
        //   "Лавина"        — knockback the player and deal 10 HP damage.
        triggerRandomEvent(p, b, inHand);
    }

    private void triggerRandomEvent(Player p, org.bukkit.block.Block b, ItemStack inHand) {
        if (Math.random() >= 0.01) return; // 1% chance.
        // Pick one of the two events.
        boolean eventIsOldPickaxe = Math.random() < 0.5;
        if (eventIsOldPickaxe) {
            // Old pickaxe — increase damage by +20.
            PickaxeType heldType = plugin.getPickaxeManager().typeOf(inHand);
            if (heldType != null && inHand.getItemMeta() instanceof Damageable) {
                Damageable dmg = (Damageable) inHand.getItemMeta();
                int current = dmg.getDamage();
                int maxDmg = inHand.getType().getMaxDurability();
                int newDamage = current + 20;
                if (newDamage >= maxDmg) {
                    // Pickaxe breaks.
                    p.getInventory().setItemInMainHand(null);
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);
                    PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
                    if (pd != null) pd.setPickaxe(PickaxeType.WOOD);
                    ItemStack fresh = plugin.getPickaxeManager().buildPickaxe(PickaxeType.WOOD);
                    p.getInventory().addItem(fresh);
                    plugin.getMessageManager().sendRaw(p,
                            plugin.getMessageManager().raw("prefix")
                                    + "&cСтарая кирка! Ваша кирка сломалась. "
                                    + "&7Выдана новая деревянная.");
                } else {
                    dmg.setDamage(newDamage);
                    inHand.setItemMeta(dmg);
                    plugin.getMessageManager().sendRaw(p,
                            plugin.getMessageManager().raw("prefix")
                                    + "&cСтарая кирка! +20 урона вашей кирке.");
                }
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
            }
        } else {
            // Avalanche — knockback + 10 damage.
            org.bukkit.util.Vector dir = p.getLocation().getDirection().multiply(-0.8);
            dir.setY(0.4);
            p.setVelocity(dir);
            p.damage(10.0);
            plugin.getMessageManager().sendRaw(p,
                    plugin.getMessageManager().raw("prefix")
                            + "&cЛавина! Вы получили 10 урона и были отброшены.");
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.6f);
            b.getWorld().spawnParticle(
                    org.bukkit.Particle.CAMPFIRE_COSY_SMOKE,
                    b.getLocation().clone().add(0.5, 0.5, 0.5),
                    30, 0.4, 0.4, 0.4, 0.1);
        }
    }
}
