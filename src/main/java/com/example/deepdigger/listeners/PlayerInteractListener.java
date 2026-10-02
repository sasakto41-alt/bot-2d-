package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.gui.GuiManager;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PickaxeType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * When a player sneak-uses a Deep Digger pickaxe, opens the pickaxe shop.
 * Otherwise the listener just exists for future use.
 */
public class PlayerInteractListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerInteractListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        // Note: shift detection is handled separately in the SneakListener.
        // This listener is here as a hook for future use.
    }
}
