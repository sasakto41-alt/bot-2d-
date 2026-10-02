package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PickaxeType;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maintains a per-player sidebar with depth / level / pickaxe / balance / workers.
 */
public class HudManager {

    private final DeepDiggerPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Objective> objectives = new HashMap<>();

    public HudManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player p) {
        ScoreboardManager sm = plugin.getServer().getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = boards.get(p.getUniqueId());
        if (board == null) {
            board = sm.getNewScoreboard();
            boards.put(p.getUniqueId(), board);
        }
        String title = plugin.getMessageManager().raw("hud-title");
        Objective obj = objectives.get(p.getUniqueId());
        if (obj == null) {
            obj = board.registerNewObjective("dd_main", "dummy", title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            objectives.put(p.getUniqueId(), obj);
        } else {
            obj.setDisplayName(title);
        }
        // Reset entries by unregistering - simpler to rebuild.
        for (String e : board.getEntries()) {
            board.resetScores(e);
        }
        PlayerData pd = plugin.getMineManager().get(p.getUniqueId());
        if (pd == null) {
            p.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
            return;
        }
        MineData md = plugin.getMineManager().getMineByOwner(p.getUniqueId());
        if (md == null && pd.isWorker()) {
            md = plugin.getMineManager().getMine(pd.getWorkingForMine());
        }
        int depth = md == null ? 0 : md.getDepth();
        int level = md == null ? 1 : md.getLevel();
        PickaxeType pickaxe = pd.getPickaxe();
        int balance = (int) pd.getBalance();
        int workers = pd.getWorkers().size();
        int max = plugin.getConfigManager().maxWorkers();
        int i = 5;
        obj.getScore(plugin.getMessageManager().raw("hud-depth", "depth", String.valueOf(depth)))
                .setScore(i--);
        obj.getScore(plugin.getMessageManager().raw("hud-mine-level", "level", roman(level)))
                .setScore(i--);
        obj.getScore(plugin.getMessageManager().raw("hud-pickaxe", "pickaxe", pickaxe.roman()))
                .setScore(i--);
        obj.getScore(plugin.getMessageManager().raw("hud-balance", "balance",
                String.valueOf(balance))).setScore(i--);
        obj.getScore(plugin.getMessageManager().raw("hud-workers", "workers",
                workers + "/" + max)).setScore(i--);
        p.setScoreboard(board);
    }

    private String roman(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            default: return String.valueOf(n);
        }
    }

    public void clear(Player p) {
        p.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
    }
}
