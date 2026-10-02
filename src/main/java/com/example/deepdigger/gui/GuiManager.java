package com.example.deepdigger.gui;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.managers.MessageManager;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PickaxeType;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds GUI menus.
 */
public class GuiManager {

    private final DeepDiggerPlugin plugin;
    public static final int SIZE = 27;

    public GuiManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player p) {
        DDHolder holder = new DDHolder(DDHolder.Type.MAIN);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                plugin.getMessageManager().raw("gui-main-title"));
        holder.setInventory(inv);
        inv.setItem(10, icon(Material.DIAMOND_PICKAXE, "&fМоя кирка", pickaxeLore(p), 1));
        inv.setItem(12, icon(Material.EMERALD, "&aУлучшить шахту", mineLore(p), 1));
        inv.setItem(14, icon(Material.PLAYER_HEAD, "&6Работники", workersLore(p), 1));
        inv.setItem(16, icon(Material.GOLD_INGOT, "&eБаланс: &a$" + (int) balance(p), balanceLore(p), 1));
        inv.setItem(22, icon(Material.BOOK, "&fИнформация", infoLore(), 1));
        p.openInventory(inv);
    }

    public void openPickaxe(Player p) {
        DDHolder holder = new DDHolder(DDHolder.Type.PICKAXE);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                plugin.getMessageManager().raw("gui-pickaxe-title"));
        holder.setInventory(inv);
        int[] slots = {10, 12, 14, 16};
        PickaxeType[] order = {PickaxeType.WOOD, PickaxeType.STONE, PickaxeType.IRON, PickaxeType.DIAMOND};
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        for (int i = 0; i < order.length; i++) {
            PickaxeType t = order[i];
            int price = plugin.getPickaxeManager().price(t);
            boolean owned = pd != null && pd.getPickaxe().ordinal() >= t.ordinal();
            String name = displayName(t);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(owned ? "&aКуплена" : "&7Цена: &a$" + price);
            lore.add("&7Прочность: &f" + plugin.getPickaxeManager().maxDurability(t));
            lore.add(owned ? "&7Текущая кирка" : "&eНажмите, чтобы купить");
            ItemStack item = icon(pickaxeMaterial(t), name, lore, 1);
            if (owned) {
                applyGlow(item);
            }
            inv.setItem(slots[i], item);
        }
        p.openInventory(inv);
    }

    public void openMine(Player p) {
        DDHolder holder = new DDHolder(DDHolder.Type.MINE);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                plugin.getMessageManager().raw("gui-mine-title"));
        holder.setInventory(inv);
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) return;
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        int curLevel = md == null ? 1 : md.getLevel();
        int curDepth = md == null ? 0 : md.getDepth();
        int nextLevel = plugin.getConfigManager().nextUpgradeLevel(curLevel);
        List<String> lore = new ArrayList<>();
        lore.add("&7Уровень: &f" + curLevel);
        lore.add("&7Глубина: &f" + curDepth + " м");
        lore.add("");
        if (nextLevel > 0) {
            int[] u = plugin.getConfigManager().upgradeFor(nextLevel);
            if (u != null) {
                lore.add("&7След. уровень: &f" + nextLevel);
                lore.add("&7Новая глубина: &f" + u[0] + " м");
                lore.add("&7Цена: &a$" + u[1]);
                lore.add("");
                lore.add("&eНажмите, чтобы купить!");
                inv.setItem(13, icon(Material.EMERALD_BLOCK, "&aУлучшить шахту", lore, 1));
            } else {
                lore.add("&7Достигнут максимальный уровень");
                inv.setItem(13, icon(Material.BARRIER, "&cУже максимум", lore, 1));
            }
        } else {
            lore.add("&7Достигнут максимальный уровень");
            inv.setItem(13, icon(Material.BARRIER, "&cУже максимум", lore, 1));
        }
        p.openInventory(inv);
    }

    public void openWorkers(Player p) {
        DDHolder holder = new DDHolder(DDHolder.Type.WORKERS);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                plugin.getMessageManager().raw("gui-workers-title"));
        holder.setInventory(inv);
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) return;
        int slot = 10;
        for (UUID id : pd.getWorkers()) {
            if (slot > 16) break;
            org.bukkit.OfflinePlayer w = plugin.getServer().getOfflinePlayer(id);
            String name = w.getName() == null ? id.toString().substring(0, 8) : w.getName();
            List<String> lore = new ArrayList<>();
            lore.add("&7Состояние: " + (w.isOnline() ? "&aВ сети" : "&cНе в сети"));
            lore.add("&7Нажмите, чтобы исключить.");
            ItemStack head = icon(Material.PLAYER_HEAD, "&f" + name, lore, 1);
            inv.setItem(slot++, head);
        }
        p.openInventory(inv);
    }

    public void openInfo(Player p) {
        DDHolder holder = new DDHolder(DDHolder.Type.INFO);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                plugin.getMessageManager().raw("gui-info-title"));
        holder.setInventory(inv);
        List<String> lore = new ArrayList<>();
        lore.add("&7Deep Digger v1.0.0");
        lore.add("&7Копайте глубже, наймите работников,");
        lore.add("&7улучшайте шахту и зарабатывайте.");
        lore.add("");
        lore.add("&e/deepdigger help");
        inv.setItem(13, icon(Material.BOOK, "&fИнформация", lore, 1));
        p.openInventory(inv);
    }

    public void openInviteAccept(Player p, com.example.deepdigger.models.Invite invite) {
        if (invite == null) {
            openMain(p);
            return;
        }
        DDHolder holder = new DDHolder(DDHolder.Type.INVITE);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                plugin.getMessageManager().raw("gui-invite-title"));
        holder.setInventory(inv);
        List<String> info = new ArrayList<>();
        info.add("&7Игрок &f" + invite.getFromName() + " &7приглашает вас");
        info.add("&7работать в его шахте.");
        info.add("");
        info.add("&7Зарплата: &a" + plugin.getConfigManager().workerPercent() + "% &7от добычи");
        inv.setItem(11, icon(Material.PAPER, "&fПриглашение", info, 1));
        inv.setItem(13, icon(Material.EMERALD_BLOCK, "&aПРИНЯТЬ", acceptLore(), 1));
        inv.setItem(15, icon(Material.REDSTONE_BLOCK, "&cОТКЛОНИТЬ", denyLore(), 1));
        p.openInventory(inv);
    }

    // ----- builders -----

    private ItemStack icon(Material material, String name, List<String> lore, int amount) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageManager.color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) colored.add(MessageManager.color(l));
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyGlow(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private Material pickaxeMaterial(PickaxeType t) {
        switch (t) {
            case WOOD: return Material.WOODEN_PICKAXE;
            case STONE: return Material.STONE_PICKAXE;
            case IRON: return Material.IRON_PICKAXE;
            case DIAMOND: return Material.DIAMOND_PICKAXE;
            default: return Material.WOODEN_PICKAXE;
        }
    }

    private String displayName(PickaxeType t) {
        switch (t) {
            case WOOD: return "&fДеревянная кирка";
            case STONE: return "&6Каменная кирка";
            case IRON: return "&fЖелезная кирка";
            case DIAMOND: return "&bАлмазная кирка";
            default: return "?";
        }
    }

    private List<String> pickaxeLore(Player p) {
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        List<String> lore = new ArrayList<>();
        if (pd != null) {
            lore.add("&7Текущая: &f" + displayName(pd.getPickaxe()));
            lore.add("&7Прочность: &f" + plugin.getPickaxeManager().maxDurability(pd.getPickaxe()));
        }
        lore.add("&eНажмите, чтобы открыть магазин");
        return lore;
    }

    private List<String> mineLore(Player p) {
        List<String> lore = new ArrayList<>();
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        int lvl = md == null ? 1 : md.getLevel();
        int depth = md == null ? 0 : md.getDepth();
        lore.add("&7Уровень: &f" + lvl);
        lore.add("&7Глубина: &f" + depth + " м");
        lore.add("&eНажмите, чтобы улучшить");
        return lore;
    }

    private List<String> workersLore(Player p) {
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        int workers = pd == null ? 0 : pd.getWorkers().size();
        int max = plugin.getConfigManager().maxWorkers();
        List<String> lore = new ArrayList<>();
        lore.add("&7Работники: &f" + workers + "/" + max);
        lore.add("&eНажмите, чтобы открыть список");
        return lore;
    }

    private List<String> balanceLore(Player p) {
        List<String> lore = new ArrayList<>();
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        double bal = pd == null ? 0 : pd.getBalance();
        lore.add("&7Баланс: &a$" + (int) bal);
        lore.add("&eНажмите для информации");
        return lore;
    }

    private List<String> infoLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&eНажмите для справки");
        return lore;
    }

    private List<String> acceptLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&aПринять приглашение");
        return lore;
    }

    private List<String> denyLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&cОтклонить приглашение");
        return lore;
    }

    private double balance(Player p) {
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        return pd == null ? 0 : pd.getBalance();
    }
}
