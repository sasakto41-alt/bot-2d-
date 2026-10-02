package com.example.deepdigger.commands;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DeepDiggerCommand implements CommandExecutor, TabCompleter {

    private final DeepDiggerPlugin plugin;

    public DeepDiggerCommand(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Default: open main menu for players.
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().send(sender, "player-only");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("deepdigger.use")) {
                plugin.getMessageManager().send(p, "no-permission");
                return true;
            }
            plugin.getGuiManager().openMain(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help": return help(sender);
            case "menu": return openMenu(sender);
            case "money": return money(sender);
            case "mine": return openMenu(sender);
            case "upgrade": return openMineUpgrade(sender);
            case "teleport":
            case "tp": return teleport(sender);
            case "invite": return invite(sender, args);
            case "accept": return accept(sender);
            case "deny": return deny(sender);
            case "kick": return kick(sender, args);
            case "members": return members(sender);
            case "admin": return admin(sender, args);
            default:
                plugin.getMessageManager().send(sender, "unknown-command");
                return true;
        }
    }

    private boolean help(CommandSender sender) {
        plugin.getMessageManager().sendRaw(sender, plugin.getMessageManager().raw("help-header"));
        sendHelpLine(sender, "deepdigger", "Открыть главное меню");
        sendHelpLine(sender, "deepdigger menu", "Открыть главное меню");
        sendHelpLine(sender, "deepdigger money", "Показать баланс");
        sendHelpLine(sender, "deepdigger mine", "Открыть меню шахты");
        sendHelpLine(sender, "deepdigger upgrade", "Меню улучшения шахты");
        sendHelpLine(sender, "deepdigger teleport", "Телепорт к своей шахте");
        sendHelpLine(sender, "deepdigger invite <ник>", "Пригласить работника");
        sendHelpLine(sender, "deepdigger accept", "Принять приглашение");
        sendHelpLine(sender, "deepdigger deny", "Отклонить приглашение");
        sendHelpLine(sender, "deepdigger kick <ник>", "Исключить работника");
        sendHelpLine(sender, "deepdigger members", "Список работников");
        sendHelpLine(sender, "deepdigger admin", "Админ-команды");
        plugin.getMessageManager().sendRaw(sender, plugin.getMessageManager().raw("help-footer"));
        return true;
    }

    private void sendHelpLine(CommandSender s, String cmd, String desc) {
        plugin.getMessageManager().sendRaw(s,
                plugin.getMessageManager().raw("help-line",
                        "cmd", cmd, "desc", desc));
    }

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("deepdigger.use")) {
            plugin.getMessageManager().send(p, "no-permission");
            return true;
        }
        if (plugin.getMineManager().getMineByOwner(p.getUniqueId()) == null) {
            plugin.getMineManager().createMineFor(p.getUniqueId(), p.getName());
            plugin.getMessageManager().send(p, "mine-created");
        }
        plugin.getGuiManager().openMain(p);
        return true;
    }

    private boolean money(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) {
            plugin.getMessageManager().send(p, "no-mine");
            return true;
        }
        plugin.getMessageManager().sendRaw(p,
                plugin.getMessageManager().raw("prefix")
                        + "&7Ваш баланс: &a$" + plugin.getEconomyManager().format(pd.getBalance()));
        return true;
    }

    private boolean openMineUpgrade(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("deepdigger.use")) {
            plugin.getMessageManager().send(p, "no-permission");
            return true;
        }
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        if (md == null) {
            plugin.getMineManager().createMineFor(p.getUniqueId(), p.getName());
            plugin.getMessageManager().send(p, "mine-created");
        }
        plugin.getGuiManager().openMine(p);
        return true;
    }

    private boolean teleport(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("deepdigger.use")) {
            plugin.getMessageManager().send(p, "no-permission");
            return true;
        }
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        if (md == null) {
            plugin.getMessageManager().send(p, "no-teleport-target");
            return true;
        }
        Location loc = plugin.getMineManager().surfaceLocation(md);
        p.teleport(loc);
        plugin.getMessageManager().send(p, "teleport-surface");
        return true;
    }

    private boolean invite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("deepdigger.use")) {
            plugin.getMessageManager().send(p, "no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.getMessageManager().sendRaw(p, "&c/deepdigger invite <ник>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageManager().send(p, "player-not-online");
            return true;
        }
        plugin.getWorkerManager().sendInvite(p, target);
        // Also try to open an invite GUI on the receiver side.
        com.example.deepdigger.models.Invite inv =
                plugin.getWorkerManager().pendingFor(target.getUniqueId());
        if (inv != null) {
            plugin.getGuiManager().openInviteAccept(target, inv);
        }
        return true;
    }

    private boolean accept(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        plugin.getWorkerManager().accept(p);
        return true;
    }

    private boolean deny(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        plugin.getWorkerManager().deny(p);
        return true;
    }

    private boolean kick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("deepdigger.use")) {
            plugin.getMessageManager().send(p, "no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.getMessageManager().sendRaw(p, "&c/deepdigger kick <ник>");
            return true;
        }
        plugin.getWorkerManager().kick(p, args[1]);
        return true;
    }

    private boolean members(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) return true;
        if (pd.getWorkers().isEmpty()) {
            plugin.getMessageManager().send(p, "worker-list-empty");
            return true;
        }
        plugin.getMessageManager().sendRaw(p, plugin.getMessageManager().raw("worker-list-header"));
        for (UUID id : pd.getWorkers()) {
            OfflinePlayer w = Bukkit.getOfflinePlayer(id);
            String name = w.getName() == null ? id.toString().substring(0, 8) : w.getName();
            plugin.getMessageManager().sendRaw(p,
                    plugin.getMessageManager().raw("worker-list-entry", "player", name));
        }
        return true;
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deepdigger.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.getMessageManager().sendRaw(sender, "&6/deepdigger admin <create|reset|delete|tp|reload> <player>");
            return true;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "reload":
                plugin.getConfigManager().reload();
                plugin.getMessageManager().send(sender, "admin-reloaded");
                return true;
            case "create":
                return adminCreate(sender, args);
            case "reset":
                return adminReset(sender, args);
            case "delete":
                return adminDelete(sender, args);
            case "recreate":
                return adminRecreate(sender, args);
            case "tp":
                return adminTp(sender, args);
            default:
                plugin.getMessageManager().sendRaw(sender, "&6/deepdigger admin <create|recreate|reset|delete|tp|reload> <player>");
                return true;
        }
    }

    private boolean adminRecreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deepdigger.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            plugin.getMessageManager().sendRaw(sender, "&c/deepdigger admin recreate <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }
        // Delete existing mine (if any) then create a fresh one.
        plugin.getMineManager().deleteMine(target.getUniqueId());
        MineData md = plugin.getMineManager().createMineFor(target.getUniqueId(),
                target.getName() == null ? args[2] : target.getName());
        plugin.getMessageManager().sendRaw(sender,
                plugin.getMessageManager().raw("prefix")
                        + "&aШахта игрока &f" + args[2] + " &aпересоздана на координатах &f("
                        + md.getSurfaceX() + ", " + md.getSurfaceY() + ", " + md.getSurfaceZ() + ")");
        // If sender is a player, teleport them to the new mine.
        if (sender instanceof Player) {
            ((Player) sender).teleport(plugin.getMineManager().surfaceLocation(md));
        }
        // If target is online, teleport them too.
        Player targetOnline = Bukkit.getPlayerExact(args[2]);
        if (targetOnline != null) {
            targetOnline.teleport(plugin.getMineManager().surfaceLocation(md));
            plugin.getMessageManager().send(targetOnline, "mine-created");
        }
        return true;
    }

    private boolean adminCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendRaw(sender, "&c/deepdigger admin create <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }
        if (plugin.getMineManager().getMineByOwner(target.getUniqueId()) != null) {
            plugin.getMessageManager().sendRaw(sender, "&cУ этого игрока уже есть шахта.");
            return true;
        }
        plugin.getMineManager().createMineFor(target.getUniqueId(),
                target.getName() == null ? args[2] : target.getName());
        plugin.getMessageManager().send(sender, "admin-mine-created", "player", args[2]);
        return true;
    }

    private boolean adminReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendRaw(sender, "&c/deepdigger admin reset <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }
        plugin.getMineManager().resetMine(target.getUniqueId());
        plugin.getMessageManager().send(sender, "admin-mine-reset", "player", args[2]);
        return true;
    }

    private boolean adminDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendRaw(sender, "&c/deepdigger admin delete <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }
        plugin.getMineManager().deleteMine(target.getUniqueId());
        plugin.getMessageManager().send(sender, "admin-mine-deleted", "player", args[2]);
        return true;
    }

    private boolean adminTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "player-only");
            return true;
        }
        Player p = (Player) sender;
        if (args.length < 3) {
            plugin.getMessageManager().sendRaw(sender, "&c/deepdigger admin tp <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        MineData md = plugin.getMineManager().getMineByOwner(target.getUniqueId());
        if (md == null) {
            plugin.getMessageManager().send(p, "no-teleport-target");
            return true;
        }
        Location loc = plugin.getMineManager().surfaceLocation(md);
        p.teleport(loc);
        plugin.getMessageManager().send(p, "admin-tp", "player", args[2]);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String[] subs = {"help","menu","money","mine","upgrade","teleport","invite",
                    "accept","deny","kick","members","admin"};
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            for (String s : Arrays.asList("create","recreate","reset","delete","tp","reload")) {
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) out.add(p.getName());
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite")
                || args[0].equalsIgnoreCase("kick"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        }
        return out;
    }
}
