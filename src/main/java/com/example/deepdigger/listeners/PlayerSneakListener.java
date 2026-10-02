package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.PickaxeType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Opens the pickaxe shop when a player sneaks while holding a Deep Digger
 * pickaxe in their main hand.
 */
public class PlayerSneakListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerSneakListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Player p = e.getPlayer();
        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() == Material.AIR) return;
        PickaxeType held = plugin.getPickaxeManager().typeOf(inHand);
        if (held == null) return;
        // Open main menu.
        plugin.getGuiManager().openMain(p);
    }
}
