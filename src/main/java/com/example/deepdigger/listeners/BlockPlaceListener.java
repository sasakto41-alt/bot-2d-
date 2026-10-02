package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Restricts placing blocks inside and around mines.
 * Only allows torches on the side walls of a mine.
 */
public class BlockPlaceListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public BlockPlaceListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (b == null) return;
        MineData md = plugin.getMineManager().getMineAt(b.getX(), b.getY(), b.getZ());
        if (md == null) {
            return;
        }
        boolean insideShaft = md.isInsideShaft(b.getX(), b.getY(), b.getZ());
        boolean isWall = md.isWall(b.getX(), b.getY(), b.getZ());
        boolean isBottom = md.isBottom(b.getX(), b.getY(), b.getZ());
        boolean isSafeZone = md.isSafeZone(b.getX(), b.getY(), b.getZ());
        if (insideShaft || isWall || isBottom || isSafeZone) {
            // Torch placement allowed on walls or inside shaft.
            if (b.getType() == Material.TORCH || b.getType() == Material.WALL_TORCH) {
                if (!plugin.getMineManager().hasAccess(e.getPlayer().getUniqueId(), md)) {
                    e.setCancelled(true);
                    plugin.getMessageManager().send(e.getPlayer(), "not-allowed-mine");
                }
                return;
            }
            e.setCancelled(true);
            return;
        }
    }
}
