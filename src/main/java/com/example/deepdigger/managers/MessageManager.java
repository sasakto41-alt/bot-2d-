package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Resolves message keys from messages.yml and applies color codes.
 */
public class MessageManager {

    private final DeepDiggerPlugin plugin;

    public MessageManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
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
        to.sendMessage(prefixed(path, pairs));
    }

    public void sendRaw(CommandSender to, String message) {
        if (to == null) return;
        to.sendMessage(color(message));
    }

    public void actionBar(Player to, String message) {
        if (to == null) return;
        to.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(color(message)));
    }

    public void title(Player to, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (to == null) return;
        to.sendTitle(title == null ? "" : color(title),
                subtitle == null ? "" : color(subtitle),
                fadeIn, stay, fadeOut);
    }

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
