package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Prevents a player from leaving the safe zone of their mine (or a mine
 * they're working in). The safe zone is:
 *
 *   - the 3x3 shaft column at any y inside the shaft, OR
 *   - the platform ring at surface level (so they can stand on top and
 *     use the holograms), OR
 *   - the air directly above the platform (for jumps)
 *
 * If a player would move to a location outside the safe zone while their
 * mine is loaded, the move is cancelled. The player is gently teleported
 * back to a safe spot when they fell off.
 *
 * This check only fires when the player crosses a block boundary so it
 * stays cheap.
 */
public class PlayerMoveListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerMoveListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        Player p = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        // Only fire when crossing a block boundary to keep this cheap.
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        // Find the player's mine (own, or worker).
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        if (md == null) {
            PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
            if (pd != null && pd.getWorkingForMine() != null) {
                md = plugin.getMineManager().getMine(pd.getWorkingForMine());
            }
        }
        if (md == null) return;

        if (isInsideSafeZone(md, to)) return;

        // Out of bounds. Cancel the move.
        e.setCancelled(true);
        // If they're below the platform, push them back up to the surface.
        if (to.getBlockY() < md.getSurfaceY()) {
            p.teleport(plugin.getMineManager().surfaceLocation(md));
        }
    }

    private boolean isInsideSafeZone(MineData md, Location loc) {
        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int h = md.halfWidth();
        int top = md.getSurfaceY();
        int bottom = md.getBottomY();
        int platformRadius = h + 1; // platform extends from -h-1 to h+1
        int fenceRadius = h + 2;    // fence ring

        int dx = loc.getBlockX() - cx;
        int dz = loc.getBlockZ() - cz;
        int adx = Math.abs(dx);
        int adz = Math.abs(dz);
        int maxd = Math.max(adx, adz);
        int y = loc.getBlockY();

        // Inside the shaft column at any valid shaft Y (or slightly above
        // the surface so they can jump in the air without getting pulled).
        if (adx <= h && adz <= h && y >= bottom - 1 && y <= top + 3) {
            return true;
        }
        // On the platform top (between the shaft and the fence).
        if (maxd <= fenceRadius && y >= top && y <= top + 3) {
            return true;
        }
        // Slightly below the platform top (so you don't get pulled when
        // standing on a slightly-low block).
        if (maxd <= fenceRadius && y == top - 1 && y >= bottom - 2) {
            return true;
        }
        return false;
    }
}
