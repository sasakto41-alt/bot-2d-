package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.gui.DDHolder;
import com.example.deepdigger.gui.GuiManager;
import com.example.deepdigger.models.Invite;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PickaxeType;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Routes GUI clicks. Closes the inventory after each action so the player
 * can interact with their normal inventory again.
 */
public class InventoryClickListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public InventoryClickListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top == null) return;
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof DDHolder)) return;
        e.setCancelled(true);
        DDHolder dd = (DDHolder) holder;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (dd.getType()) {
            case MAIN: handleMain(p, e.getSlot()); break;
            case PICKAXE: handlePickaxe(p, e.getSlot()); break;
            case MINE: handleMine(p, e.getSlot()); break;
            case WORKERS: handleWorkers(p, e.getSlot()); break;
            case INFO: p.closeInventory(); break;
            case INVITE: handleInvite(p, e.getSlot()); break;
        }
    }

    private void handleMain(Player p, int slot) {
        switch (slot) {
            case 10:
                p.closeInventory();
                plugin.getGuiManager().openPickaxe(p);
                break;
            case 12:
                p.closeInventory();
                plugin.getGuiManager().openMine(p);
                break;
            case 14:
                p.closeInventory();
                plugin.getGuiManager().openWorkers(p);
                break;
            case 16:
                p.closeInventory();
                plugin.getGuiManager().openInfo(p);
                break;
            case 22:
                p.closeInventory();
                p.performCommand("deepdigger help");
                break;
        }
    }

    private void handlePickaxe(Player p, int slot) {
        int[] slots = {10, 12, 14, 16};
        PickaxeType[] order = {PickaxeType.WOOD, PickaxeType.STONE, PickaxeType.IRON, PickaxeType.DIAMOND};
        int idx = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) { idx = i; break; }
        }
        if (idx < 0) return;
        PickaxeType t = order[idx];
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) { p.closeInventory(); return; }
        if (pd.getPickaxe().ordinal() >= t.ordinal()) {
            plugin.getMessageManager().send(p, "pickaxe-already-owned");
            return;
        }
        int price = plugin.getPickaxeManager().price(t);
        if (!plugin.getEconomyManager().has(pd, price)) {
            plugin.getMessageManager().send(p, "not-enough-money");
            return;
        }
        // Must buy the next tier in sequence, not skip.
        if (t.ordinal() != pd.getPickaxe().ordinal() + 1) {
            plugin.getMessageManager().send(p, "better-pickaxe");
            return;
        }
        plugin.getEconomyManager().take(pd, price);
        pd.setPickaxe(t);
        // Give item.
        ItemStack pick = plugin.getPickaxeManager().buildPickaxe(t);
        p.getInventory().addItem(pick);
        p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfigManager().pickaxeBuySound()), 1f, 1f);
        plugin.getMessageManager().send(p, "pickaxe-bought", "pickaxe",
                displayName(t));
        p.closeInventory();
        plugin.getGuiManager().openPickaxe(p);
    }

    private void handleMine(Player p, int slot) {
        if (slot != 13) return;
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) { p.closeInventory(); return; }
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        if (md == null) {
            plugin.getMessageManager().send(p, "no-mine");
            p.closeInventory();
            return;
        }
        int curLevel = md.getLevel();
        int nextLevel = plugin.getConfigManager().nextUpgradeLevel(curLevel);
        if (nextLevel <= 0) {
            plugin.getMessageManager().send(p, "mine-already-max");
            return;
        }
        int[] u = plugin.getConfigManager().upgradeFor(nextLevel);
        if (u == null) {
            plugin.getMessageManager().send(p, "mine-already-max");
            return;
        }
        int price = u[1];
        int newDepth = u[0];
        if (!plugin.getEconomyManager().has(pd, price)) {
            plugin.getMessageManager().send(p, "not-enough-money");
            return;
        }
        plugin.getEconomyManager().take(pd, price);
        int oldDepth = md.getDepth();
        md.setLevel(nextLevel);
        md.setDepth(newDepth);
        pd.setMineLevel(nextLevel);
        pd.setMineDepth(newDepth);
        plugin.getMineManager().extendShaft(md, oldDepth, newDepth);
        p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfigManager().upgradeSound()), 1f, 1f);
        plugin.getMessageManager().send(p, "mine-upgraded", "depth", String.valueOf(newDepth));
        p.closeInventory();
        plugin.getGuiManager().openMine(p);
    }

    private void handleWorkers(Player p, int slot) {
        if (slot < 10 || slot > 16) return;
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) { p.closeInventory(); return; }
        // Map slot -> worker index (10->0, 12->1, 14->2, 16->3)
        int[] slots = {10, 12, 14, 16};
        int idx = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) { idx = i; break; }
        }
        if (idx < 0 || idx >= pd.getWorkers().size()) return;
        java.util.UUID workerId = pd.getWorkers().get(idx);
        org.bukkit.OfflinePlayer off = plugin.getServer().getOfflinePlayer(workerId);
        String name = off.getName() == null ? workerId.toString().substring(0, 8) : off.getName();
        plugin.getWorkerManager().kick(p, name);
        p.closeInventory();
        plugin.getGuiManager().openWorkers(p);
    }

    private void handleInvite(Player p, int slot) {
        Invite inv = plugin.getWorkerManager().pendingFor(p.getUniqueId());
        if (inv == null) {
            p.closeInventory();
            return;
        }
        if (slot == 13) {
            p.closeInventory();
            plugin.getWorkerManager().accept(p);
        } else if (slot == 15) {
            p.closeInventory();
            plugin.getWorkerManager().deny(p);
        }
    }

    private String displayName(PickaxeType t) {
        switch (t) {
            case WOOD: return "Деревянная кирка";
            case STONE: return "Каменная кирка";
            case IRON: return "Железная кирка";
            case DIAMOND: return "Алмазная кирка";
            default: return "?";
        }
    }
}
