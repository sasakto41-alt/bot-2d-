package com.example.deepdigger.tasks;

import com.example.deepdigger.DeepDiggerPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SaveTask extends BukkitRunnable {

    private final DeepDiggerPlugin plugin;

    public SaveTask(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getMineManager().saveAll();
    }
}
