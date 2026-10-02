package com.example.deepdigger;

import com.example.deepdigger.commands.DeepDiggerCommand;
import com.example.deepdigger.gui.GuiManager;
import com.example.deepdigger.listeners.*;
import com.example.deepdigger.managers.*;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.tasks.*;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for the Deep Digger plugin.
 */
public class DeepDiggerPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private MineManager mineManager;
    private EconomyManager economyManager;
    private PickaxeManager pickaxeManager;
    private OreManager oreManager;
    private RegenManager regenManager;
    private WorkerManager workerManager;
    private HudManager hudManager;
    private GuiManager guiManager;
    private HologramManager hologramManager;

    private NamespacedKey holoActionKey;
    private NamespacedKey holoMineKey;

    private HudUpdateTask hudUpdateTask;
    private DarknessTask darknessTask;
    private SaveTask saveTask;

    @Override
    public void onEnable() {
        // Config first.
        configManager = new ConfigManager(this);
        configManager.load();
        messageManager = new MessageManager(this);

        // Core managers.
        mineManager = new MineManager(this);
        mineManager.loadAll();
        economyManager = new EconomyManager(this);
        pickaxeManager = new PickaxeManager(this);
        pickaxeManager.init();
        oreManager = new OreManager(this);
        regenManager = new RegenManager(this);
        regenManager.start();
        workerManager = new WorkerManager(this);
        hudManager = new HudManager(this);
        guiManager = new GuiManager(this);
        hologramManager = new HologramManager(this);
        holoActionKey = new NamespacedKey(this, "holo_action");
        holoMineKey = new NamespacedKey(this, "holo_mine");

        // Listeners.
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSneakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractAtEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);

        // Command.
        DeepDiggerCommand cmd = new DeepDiggerCommand(this);
        getCommand("deepdigger").setExecutor(cmd);
        getCommand("deepdigger").setTabCompleter(cmd);

        // Tasks.
        hudUpdateTask = new HudUpdateTask(this);
        hudUpdateTask.runTaskTimer(this, 20L, configManager.hudUpdateTicks());
        darknessTask = new DarknessTask(this);
        darknessTask.runTaskTimer(this, 40L, configManager.darknessCheckPeriod());
        saveTask = new SaveTask(this);
        saveTask.runTaskTimer(this, configManager.autosaveSeconds() * 20L,
                configManager.autosaveSeconds() * 20L);

        // Create a mine for everyone who has no mine (recovered data on startup).
        for (var p : getServer().getOnlinePlayers()) {
            if (mineManager.getMineByOwner(p.getUniqueId()) == null) {
                mineManager.createMineFor(p.getUniqueId(), p.getName());
                messageManager.send(p, "mine-created");
            }
            getHudManager().apply(p);
        }

        // Re-spawn holograms for all existing mines (they vanish on server stop).
        for (MineData md : mineManager.all().values()) {
            hologramManager.spawnFor(md);
        }

        getLogger().info("Deep Digger enabled.");
    }

    @Override
    public void onDisable() {
        if (mineManager != null) {
            mineManager.saveAll();
        }
        if (regenManager != null) {
            regenManager.stop();
        }
        getLogger().info("Deep Digger disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public MineManager getMineManager() { return mineManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public PickaxeManager getPickaxeManager() { return pickaxeManager; }
    public OreManager getOreManager() { return oreManager; }
    public RegenManager getRegenManager() { return regenManager; }
    public WorkerManager getWorkerManager() { return workerManager; }
    public HudManager getHudManager() { return hudManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public NamespacedKey getHoloActionKey() { return holoActionKey; }
    public NamespacedKey getHoloMineKey() { return holoMineKey; }
}
