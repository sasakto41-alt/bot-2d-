package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Prevents a player from leaving the safe zone of their mine (or a mine
 * they're working in). The safe zone is:
 *
 *   - the 3x3 shaft column at any y inside the shaft
 *   - the platform top + an air column above (so they can jump without
 *     getting pulled), bounded horizontally to the fence ring radius
 *
 * If a player would move to a location outside the safe zone while their
 * mine is loaded, the move is cancelled. Teleports that try to land near
 * the mine but outside the safe zone are also cancelled and the player
 * is snapped back to the surface.
 *
 * The move check only fires when the player crosses a block boundary so
 * it stays cheap.
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

        MineData md = findMine(p);
        if (md == null) return;

        if (isInsideSafeZone(md, to)) return;

        // Out of bounds. Cancel the move and teleport to a safe spot.
        e.setCancelled(true);
        // If they fell far below the platform, teleport them up to the surface.
        if (to.getBlockY() < md.getSurfaceY() - 5) {
            p.teleport(plugin.getMineManager().surfaceLocation(md));
        }
    }

    /**
     * Catches teleport-based escapes (ender pearls, chorus fruit, admin
     * teleports to outside the safe zone, etc).
     */
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) return;
        Player p = e.getPlayer();
        MineData md = findMine(p);
        if (md == null) return;
        // If player is teleporting TO a location that's inside the safe zone,
        // allow it.
        if (isInsideSafeZone(md, e.getTo())) return;
        // If the destination is far from the mine (more than 50 blocks
        // horizontally), assume the player is leaving the mine intentionally
        // (e.g., /spawn) and allow it.
        int dx = e.getTo().getBlockX() - md.getSurfaceX();
        int dz = e.getTo().getBlockZ() - md.getSurfaceZ();
        if (Math.abs(dx) > 50 || Math.abs(dz) > 50) return;
        // Otherwise cancel and snap them back to the surface.
        e.setCancelled(true);
        p.teleport(plugin.getMineManager().surfaceLocation(md));
    }

    private MineData findMine(Player p) {
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        if (md == null) {
            PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
            if (pd != null && pd.getWorkingForMine() != null) {
                md = plugin.getMineManager().getMine(pd.getWorkingForMine());
            }
        }
        return md;
    }

    private boolean isInsideSafeZone(MineData md, Location loc) {
        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int h = md.halfWidth();
        int top = md.getSurfaceY();
        int bottom = md.getBottomY();
        int fenceRadius = h + 2;

        int dx = loc.getBlockX() - cx;
        int dz = loc.getBlockZ() - cz;
        int adx = Math.abs(dx);
        int adz = Math.abs(dz);
        int maxd = Math.max(adx, adz);
        int y = loc.getBlockY();

        // Above the platform (or on the platform top) — strictly bounded to
        // the fence ring so the player cannot jump over the fence.
        if (y >= top) {
            return maxd <= fenceRadius;
        }
        // Inside the shaft column.
        if (y >= bottom - 1 && y < top) {
            return adx <= h && adz <= h;
        }
        // Below the bedrock floor — they shouldn't be here. Block it.
        return false;
    }
}
