package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Internal currency. No Vault.
 * Balances live inside PlayerData so they're saved alongside player state.
 */
public class EconomyManager {

    private final DeepDiggerPlugin plugin;
    private static final DecimalFormat FMT;

    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.US);
        s.setGroupingSeparator(',');
        FMT = new DecimalFormat("#,##0.##", s);
    }

    public EconomyManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    public double balance(PlayerData pd) {
        return pd == null ? 0 : pd.getBalance();
    }

    public boolean has(PlayerData pd, double amount) {
        return pd != null && pd.getBalance() >= amount - 1e-6;
    }

    public void give(PlayerData pd, double amount) {
        if (pd == null || amount <= 0) return;
        pd.setBalance(pd.getBalance() + amount);
    }

    public boolean take(PlayerData pd, double amount) {
        if (pd == null || amount <= 0) return false;
        if (pd.getBalance() < amount - 1e-6) return false;
        pd.setBalance(pd.getBalance() - amount);
        return true;
    }

    /**
     * Distributes reward between worker and owner. If the player is mining
     * their own mine, the full amount goes to the player.
     */
    public void payMiningReward(PlayerData miner, double reward) {
        if (miner == null) return;
        if (!miner.isWorker()) {
            give(miner, reward);
            return;
        }
        String workingFor = miner.getWorkingForMine();
        if (workingFor == null) {
            give(miner, reward);
            return;
        }
        com.example.deepdigger.models.MineData md = plugin.getMineManager().getMine(workingFor);
        if (md == null) {
            give(miner, reward);
            return;
        }
        PlayerData owner = plugin.getMineManager().get(md.getOwner());
        double workerPct = plugin.getConfigManager().workerPercent();
        double ownerPct = plugin.getConfigManager().ownerPercent();
        double total = workerPct + ownerPct;
        if (total <= 0) {
            give(miner, reward);
            return;
        }
        double workerPart = reward * (workerPct / total);
        double ownerPart = reward * (ownerPct / total);
        give(miner, workerPart);
        if (owner != null) {
            give(owner, ownerPart);
            Player ownerOnline = plugin.getServer().getPlayer(owner.getUuid());
            if (ownerOnline != null) {
                plugin.getMessageManager().send(ownerOnline,
                        "owner-earned", "amount", format(ownerPart));
            }
        }
    }

    public String format(double amount) {
        return FMT.format(amount);
    }
}
