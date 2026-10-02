package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Central place for reading and rewriting YAML files that ship with the plugin.
 * Two sources:
 *  - config.yml  (data exposed to admins)
 *  - messages.yml (strings shown to players)
 */
public class ConfigManager {

    private final DeepDiggerPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    private File configFile;
    private File messagesFile;

    public ConfigManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        // Merge defaults from the packaged resource so new keys appear.
        InputStream defaults = plugin.getResource("config.yml");
        if (defaults != null) {
            YamlConfiguration defaultsCfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8));
            config.setDefaults(defaultsCfg);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream mDefaults = plugin.getResource("messages.yml");
        if (mDefaults != null) {
            YamlConfiguration mDef = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(mDefaults, StandardCharsets.UTF_8));
            messages.setDefaults(mDef);
        }
        plugin.getLogger().info("Config loaded.");
    }

    public void save() {
        try {
            config.save(configFile);
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configs", e);
        }
    }

    public void reload() {
        load();
    }

    public FileConfiguration config() { return config; }
    public FileConfiguration messages() { return messages; }

    // ----- accessors -----

    public String worldName() {
        return config.getString("world.name", "world");
    }

    public int spacingX() {
        return config.getInt("world.spacing-x", 100);
    }

    public int spacingZ() {
        return config.getInt("world.spacing-z", 0);
    }

    public int surfaceY() {
        return config.getInt("world.surface-y", 64);
    }

    public int startDepth() {
        return config.getInt("mine.start-depth", 30);
    }

    public int maxWorkers() {
        return config.getInt("mine.max-workers", 5);
    }

    public int absoluteMaxDepth() {
        return config.getInt("mine.absolute-max-depth", 320);
    }

    public double startingBalance() {
        return config.getDouble("economy.starting-balance", 0.0);
    }

    public double workerPercent() {
        return config.getDouble("economy.worker-percent", 70.0);
    }

    public double ownerPercent() {
        return config.getDouble("economy.owner-percent", 30.0);
    }

    public int regenDelaySeconds() {
        return config.getInt("regeneration.delay-seconds", 3);
    }

    public int torchRadius() {
        return config.getInt("lighting.torch-radius", 5);
    }

    public boolean darknessEnabled() {
        return config.getBoolean("lighting.darkness-enabled", true);
    }

    public int darknessCheckPeriod() {
        return config.getInt("lighting.check-period-ticks", 40);
    }

    public boolean darknessFallbackBlindness() {
        return config.getBoolean("lighting.fallback-blindness", true);
    }

    public String wallBlock() {
        return config.getString("wall-block", "GLASS");
    }

    public String surfaceBlock() {
        return config.getString("surface-block", "GRASS_BLOCK");
    }

    public String bottomBlock() {
        return config.getString("bottom-block", "BEDROCK");
    }

    public int shaftWidth() {
        int w = config.getInt("mine.shaft-width", 3);
        if (w < 1) w = 1;
        if (w % 2 == 0) w += 1; // odd only
        return w;
    }

    public int hudUpdateTicks() {
        return config.getInt("tasks.hud-update-ticks", 20);
    }

    public int autosaveSeconds() {
        return config.getInt("tasks.autosave-seconds", 120);
    }

    public String rareParticle() {
        return config.getString("effects.rare-ore-particle", "CRIT");
    }

    public String rareSound() {
        return config.getString("effects.rare-ore-sound", "BLOCK_NOTE_BLOCK_PLING");
    }

    public String commonSound() {
        return config.getString("effects.common-ore-sound", "BLOCK_STONE_BREAK");
    }

    public String moneySound() {
        return config.getString("effects.money-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public String pickaxeBuySound() {
        return config.getString("effects.pickaxe-buy-sound", "BLOCK_ANVIL_USE");
    }

    public String upgradeSound() {
        return config.getString("effects.upgrade-sound", "BLOCK_BEACON_POWER_SELECT");
    }

    /**
     * Returns the upgrade table as an ordered map level -> {depth, price}.
     * Falls back to the spec defaults if the section is missing.
     */
    public Map<Integer, int[]> upgradeTable() {
        Map<Integer, int[]> out = new LinkedHashMap<>();
        ConfigurationSection sec = config.getConfigurationSection("mine.upgrades");
        if (sec == null) {
            out.put(2, new int[]{50, 1000});
            out.put(3, new int[]{75, 2500});
            out.put(4, new int[]{100, 5000});
            out.put(5, new int[]{150, 10000});
            out.put(6, new int[]{200, 25000});
            out.put(7, new int[]{300, 50000});
            return out;
        }
        for (String key : sec.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                int depth = sec.getInt(key + ".depth");
                int price = sec.getInt(key + ".price");
                out.put(level, new int[]{depth, price});
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public int nextUpgradeLevel(int currentLevel) {
        Map<Integer, int[]> table = upgradeTable();
        int next = -1;
        for (int lvl : table.keySet()) {
            if (lvl > currentLevel && (next == -1 || lvl < next)) {
                next = lvl;
            }
        }
        return next;
    }

    public int[] upgradeFor(int level) {
        return upgradeTable().get(level);
    }

    public int maxConfiguredLevel() {
        int max = 1;
        for (int lvl : upgradeTable().keySet()) {
            if (lvl > max) max = lvl;
        }
        return max;
    }

    /**
     * Returns the ore section as a list of maps so callers don't have to
     * touch ConfigurationSection every time. Each entry has keys:
     * name, block, min-depth, max-depth, chance, reward, required-pickaxe.
     */
    public List<Map<String, Object>> oreList() {
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        ConfigurationSection sec = config.getConfigurationSection("ores");
        if (sec == null) return out;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection ore = sec.getConfigurationSection(key);
            if (ore == null) continue;
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", key);
            entry.put("block", ore.getString("block", "STONE"));
            entry.put("min-depth", ore.getInt("min-depth", 0));
            entry.put("max-depth", ore.getInt("max-depth", 320));
            entry.put("chance", ore.getDouble("chance", 0.0));
            entry.put("reward", ore.getDouble("reward", 0.0));
            entry.put("required-pickaxe", ore.getString("required-pickaxe", "WOOD"));
            out.add(entry);
        }
        return out;
    }

    /**
     * Returns pickaxe catalog entries keyed by PickaxeType name.
     */
    public Map<String, Map<String, Object>> pickaxeCatalog() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        ConfigurationSection sec = config.getConfigurationSection("pickaxes");
        if (sec == null) return out;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection pe = sec.getConfigurationSection(key);
            if (pe == null) continue;
            Map<String, Object> entry = new HashMap<>();
            entry.put("display", pe.getString("display", key));
            entry.put("lore", pe.getStringList("lore"));
            entry.put("price", pe.getDouble("price", 0.0));
            entry.put("durability", pe.getInt("durability", 100));
            entry.put("material", pe.getString("material", "WOODEN_PICKAXE"));
            out.put(key, entry);
        }
        return out;
    }

    // ----- messages -----

    public String msg(String path) {
        String v = messages.getString(path, null);
        return v == null ? "" : v;
    }

    public String msg(String path, String... pairs) {
        String v = messages.getString(path, null);
        if (v == null) return "";
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            v = v.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return v;
    }

    public String prefixed(String path, String... pairs) {
        return msg("prefix") + msg(path, pairs);
    }
}
