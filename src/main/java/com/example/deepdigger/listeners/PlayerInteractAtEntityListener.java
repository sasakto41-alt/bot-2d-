package com.example.deepdigger.listeners;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.managers.HologramManager;
import com.example.deepdigger.models.MineData;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/**
 * Routes right-click events on Deep Digger holograms (invisible ArmorStands
 * sitting on the fence around each mine's platform). The action depends on
 * which hologram was clicked.
 */
public class PlayerInteractAtEntityListener implements Listener {

    private final DeepDiggerPlugin plugin;

    public PlayerInteractAtEntityListener(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() == null) return;
        Player p = e.getPlayer();
        HologramManager.Action action = plugin.getHologramManager().actionFor(e.getRightClicked());
        if (action == null) return;
        // Cancel the interaction so the armor stand doesn't do anything weird.
        e.setCancelled(true);
        if (!p.hasPermission("deepdigger.use")) {
            plugin.getMessageManager().send(p, "no-permission");
            return;
        }
        String mineKey = plugin.getHologramManager().mineKeyFor(e.getRightClicked());
        MineData md = mineKey == null ? null : plugin.getMineManager().getMine(mineKey);
        if (md == null) return;
        // Only the owner (or workers with access) can use the holograms.
        if (!plugin.getMineManager().hasAccess(p.getUniqueId(), md)) {
            plugin.getMessageManager().send(p, "not-allowed-mine");
            return;
        }
        switch (action) {
            case MENU:
                plugin.getGuiManager().openMain(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                break;
            case UPGRADE:
                plugin.getGuiManager().openMine(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                break;
            case PICKAXE:
                plugin.getGuiManager().openPickaxe(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                break;
            case REFRESH_MINE:
                // Only the owner can refresh the mine (worker abuse protection).
                if (!md.isOwner(p.getUniqueId())) {
                    plugin.getMessageManager().send(p, "not-mine-owner");
                    return;
                }
                plugin.getMineManager().refreshMine(md);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1f, 0.8f);
                plugin.getMessageManager().sendRaw(p,
                        plugin.getMessageManager().raw("prefix")
                                + "&aШахта обновлена — все блоки восстановлены.");
                break;
            case TELEPORT_TOP:
                // Teleport the player back to the surface of this mine.
                org.bukkit.Location loc = plugin.getMineManager().surfaceLocation(md);
                p.teleport(loc);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                plugin.getMessageManager().sendRaw(p,
                        plugin.getMessageManager().raw("prefix")
                                + "&aТелепорт на поверхность.");
                break;
        }
    }
}
