package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.Invite;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles worker invites. Invites expire after {@link #EXPIRE_MS} millis.
 */
public class WorkerManager {

    private final DeepDiggerPlugin plugin;
    private final Map<String, Invite> pending = new HashMap<>();
    private static final long EXPIRE_MS = 60_000L;

    public WorkerManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Send an invite from one player to another.
     */
    public boolean sendInvite(Player from, Player to) {
        if (from == null || to == null) return false;
        if (from.getUniqueId().equals(to.getUniqueId())) {
            plugin.getMessageManager().send(from, "no-self-invite");
            return false;
        }
        PlayerData fromData = plugin.getMineManager().get(from.getUniqueId());
        PlayerData toData = plugin.getMineManager().get(to.getUniqueId());
        if (fromData == null) return false;
        MineData md = plugin.getMineManager().getMineByOwner(from.getUniqueId());
        if (md == null) {
            plugin.getMessageManager().send(from, "no-mine");
            return false;
        }
        if (fromData.getWorkers().size() >= plugin.getConfigManager().maxWorkers()) {
            plugin.getMessageManager().send(from, "max-workers-reached");
            return false;
        }
        if (toData != null && toData.getWorkingForMine() != null) {
            // Player already works somewhere. We allow invite but they must
            // leave their current job before accepting.
        }
        if (fromData.getWorkers().contains(to.getUniqueId())) {
            plugin.getMessageManager().send(from, "player-already-worker");
            return false;
        }
        // Create invite.
        String id = "inv_" + Long.toHexString(System.currentTimeMillis())
                + "_" + Integer.toHexString(to.getUniqueId().hashCode());
        Invite invite = new Invite(id, from.getUniqueId(), to.getUniqueId(),
                from.getName(), to.getName(), System.currentTimeMillis());
        pending.put(id, invite);
        plugin.getMessageManager().send(from, "invite-sent", "player", to.getName());
        // Notify receiver.
        plugin.getMessageManager().title(to,
                plugin.getConfigManager().msg("invite-received-title"),
                plugin.getConfigManager().msg("invite-received-subtitle", "player", from.getName()),
                10, 60, 10);
        plugin.getMessageManager().send(to, "invite-received-message", "player", from.getName());
        return true;
    }

    public Invite pendingFor(UUID uuid) {
        // Expire stale.
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Invite>> it = pending.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Invite> e = it.next();
            if (now - e.getValue().getCreatedAt() > EXPIRE_MS) {
                it.remove();
            }
        }
        for (Invite inv : pending.values()) {
            if (inv.getToUuid().equals(uuid)) return inv;
        }
        return null;
    }

    public boolean accept(Player acceptor) {
        Invite inv = pendingFor(acceptor.getUniqueId());
        if (inv == null) {
            plugin.getMessageManager().send(acceptor, "no-pending-invite");
            return false;
        }
        // Find owner.
        PlayerData ownerData = plugin.getMineManager().get(inv.getFromUuid());
        if (ownerData == null) {
            plugin.getMessageManager().send(acceptor, "no-pending-invite");
            pending.remove(inv.getId());
            return false;
        }
        MineData md = plugin.getMineManager().getMineByOwner(inv.getFromUuid());
        if (md == null) {
            plugin.getMessageManager().send(acceptor, "no-pending-invite");
            pending.remove(inv.getId());
            return false;
        }
        if (ownerData.getWorkers().size() >= plugin.getConfigManager().maxWorkers()) {
            plugin.getMessageManager().send(acceptor, "max-workers-reached");
            pending.remove(inv.getId());
            return false;
        }
        // Move acceptor: remove from any previous job.
        PlayerData acc = plugin.getMineManager().getOrCreate(acceptor.getUniqueId(), acceptor.getName());
        if (acc.getWorkingForMine() != null) {
            PlayerData prevOwner = plugin.getMineManager().get(inv.getFromUuid());
            // already same owner? skip
            if (acc.getWorkingForMine().equals(md.getKey())) {
                plugin.getMessageManager().send(acceptor, "player-already-worker");
                pending.remove(inv.getId());
                return false;
            }
            // Remove from previous owner's worker list.
            PlayerData oldOwner = null;
            MineData oldMd = plugin.getMineManager().getMine(acc.getWorkingForMine());
            if (oldMd != null) {
                oldOwner = plugin.getMineManager().get(oldMd.getOwner());
                if (oldOwner != null) {
                    oldOwner.getWorkers().remove(acceptor.getUniqueId());
                }
            }
        }
        // Add to new owner.
        ownerData.getWorkers().add(acceptor.getUniqueId());
        acc.setWorkingForMine(md.getKey());
        pending.remove(inv.getId());
        plugin.getMessageManager().send(acceptor, "invite-accepted", "player", inv.getFromName());
        Player ownerOnline = plugin.getServer().getPlayer(inv.getFromUuid());
        if (ownerOnline != null) {
            plugin.getMessageManager().send(ownerOnline, "invite-owner-notify", "player", acceptor.getName());
        }
        return true;
    }

    public boolean deny(Player denier) {
        Invite inv = pendingFor(denier.getUniqueId());
        if (inv == null) {
            plugin.getMessageManager().send(denier, "no-pending-invite");
            return false;
        }
        pending.remove(inv.getId());
        plugin.getMessageManager().send(denier, "invite-denied");
        return true;
    }

    public boolean kick(Player owner, String targetName) {
        PlayerData ownerData = plugin.getMineManager().get(owner.getUniqueId());
        if (ownerData == null) return false;
        MineData md = plugin.getMineManager().getMineByOwner(owner.getUniqueId());
        if (md == null) {
            plugin.getMessageManager().send(owner, "no-mine");
            return false;
        }
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        if (target == null) {
            plugin.getMessageManager().send(owner, "player-not-found");
            return false;
        }
        if (!ownerData.getWorkers().contains(target.getUniqueId())) {
            plugin.getMessageManager().send(owner, "not-a-worker");
            return false;
        }
        ownerData.getWorkers().remove(target.getUniqueId());
        PlayerData td = plugin.getMineManager().get(target.getUniqueId());
        if (td != null) {
            td.setWorkingForMine(null);
        }
        plugin.getMessageManager().send(owner, "kicked-worker", "player", targetName);
        if (target.isOnline()) {
            plugin.getMessageManager().send((Player) target, "worker-kicked-notify", "owner", owner.getName());
        }
        return true;
    }
}
