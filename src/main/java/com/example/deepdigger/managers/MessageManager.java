package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Resolves message keys from messages.yml and applies color codes.
 * Honors per-player notification toggle: certain "spammy" messages are
 * suppressed when the player has disabled notifications.
 */
public class MessageManager {

    private final DeepDiggerPlugin plugin;

    public MessageManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns true if the given path is a spammy mining/economy message
     * that the player can disable via /deepdigger notify off.
     */
    private boolean isSpammyPath(String path) {
        if (path == null) return false;
        switch (path) {
            case "mined-ore":
            case "mined-stone":
            case "money-earned":
            case "worker-earned":
            case "owner-earned":
                return true;
            default:
                return false;
        }
    }

    private boolean shouldSuppress(CommandSender to, String path) {
        if (!(to instanceof Player)) return false;
        Player p = (Player) to;
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) return false;
        if (pd.isNotificationsEnabled()) return false;
        return isSpammyPath(path);
    }

    public String raw(String path, String... pairs) {
        return color(plugin.getConfigManager().msg(path, pairs));
    }

    public String prefixed(String path, String... pairs) {
        String prefix = plugin.getConfigManager().msg("prefix");
        return color(prefix) + color(plugin.getConfigManager().msg(path, pairs));
    }

    public void send(CommandSender to, String path, String... pairs) {
        if (to == null) return;
        if (shouldSuppress(to, path)) return;
        to.sendMessage(prefixed(path, pairs));
    }

    public void sendRaw(CommandSender to, String message) {
        if (to == null) return;
        to.sendMessage(color(message));
    }

    public void actionBar(Player to, String message) {
        if (to == null) return;
        // Honor per-player notification toggle: action bars are suppressed
        // when notifications are off.
        PlayerData pd = plugin.getMineManager().get(to.getUniqueId());
        if (pd != null && !pd.isNotificationsEnabled()) return;
        to.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(color(message)));
    }

    public void title(Player to, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (to == null) return;
        // Title messages are important (e.g., invite notifications) — always show.
        to.sendTitle(title == null ? "" : color(title),
                subtitle == null ? "" : color(subtitle),
                fadeIn, stay, fadeOut);
    }

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
