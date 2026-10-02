package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Builds and stores personal mines.
 * <p>
 * Every player gets exactly one mine at coordinates (nx * spacing, surfaceY, 0).
 * Storage: data/mines.yml + data/players.yml
 */
public class MineManager {

    private final DeepDiggerPlugin plugin;
    private final File minesFile;
    private final File playersFile;
    private FileConfiguration minesConfig;
    private FileConfiguration playersConfig;

    private final Map<String, MineData> mines = new HashMap<>();
    private final Map<UUID, PlayerData> players = new HashMap<>();

    private int nextMineIndex = 1;

    public MineManager(DeepDiggerPlugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create data dir");
        }
        this.minesFile = new File(dir, "mines.yml");
        this.playersFile = new File(dir, "players.yml");
    }

    // ----- loading / saving -----

    public void loadAll() {
        try {
            if (!minesFile.exists()) minesFile.createNewFile();
            if (!playersFile.exists()) playersFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create data files", e);
        }
        minesConfig = YamlConfiguration.loadConfiguration(minesFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);

        // Load mines.
        ConfigurationSection ms = minesConfig.getConfigurationSection("mines");
        if (ms != null) {
            for (String key : ms.getKeys(false)) {
                ConfigurationSection m = ms.getConfigurationSection(key);
                if (m == null) continue;
                MineData md = new MineData();
                md.setKey(key);
                String owner = m.getString("owner");
                md.setOwner(owner == null ? null : UUID.fromString(owner));
                md.setSurfaceX(m.getInt("surfaceX"));
                md.setSurfaceY(m.getInt("surfaceY"));
                md.setSurfaceZ(m.getInt("surfaceZ"));
                md.setLevel(m.getInt("level", 1));
                md.setDepth(m.getInt("depth", 30));
                mines.put(key, md);
                // Track highest index for nextMineIndex.
                try {
                    int idx = Integer.parseInt(key.replace("mine_", ""));
                    if (idx >= nextMineIndex) nextMineIndex = idx + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        plugin.getLogger().info("Loaded " + mines.size() + " mines.");

        // Load players.
        ConfigurationSection ps = playersConfig.getConfigurationSection("players");
        if (ps != null) {
            for (String key : ps.getKeys(false)) {
                ConfigurationSection p = ps.getConfigurationSection(key);
                if (p == null) continue;
                PlayerData pd = new PlayerData();
                try {
                    pd.setUuid(UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                pd.setName(p.getString("name", "?"));
                pd.setBalance(p.getDouble("balance", 0.0));
                pd.setMineLevel(p.getInt("mineLevel", 1));
                pd.setMineDepth(p.getInt("mineDepth", plugin.getConfigManager().startDepth()));
                String pickaxe = p.getString("pickaxe", "WOOD");
                try {
                    pd.setPickaxe(com.example.deepdigger.models.PickaxeType.valueOf(pickaxe));
                } catch (IllegalArgumentException ex) {
                    pd.setPickaxe(com.example.deepdigger.models.PickaxeType.WOOD);
                }
                pd.setMineKey(p.getString("mineKey", null));
                pd.setWorkingForMine(p.getString("workingForMine", null));
                java.util.List<UUID> workers = new java.util.ArrayList<>();
                for (String w : p.getStringList("workers")) {
                    try { workers.add(UUID.fromString(w)); } catch (IllegalArgumentException ignored) {}
                }
                pd.setWorkers(workers);
                players.put(pd.getUuid(), pd);
            }
        }
        plugin.getLogger().info("Loaded " + players.size() + " players.");
    }

    public void saveAll() {
        // Mines.
        for (MineData md : mines.values()) {
            String path = "mines." + md.getKey();
            minesConfig.set(path + ".owner", md.getOwner() == null ? null : md.getOwner().toString());
            minesConfig.set(path + ".surfaceX", md.getSurfaceX());
            minesConfig.set(path + ".surfaceY", md.getSurfaceY());
            minesConfig.set(path + ".surfaceZ", md.getSurfaceZ());
            minesConfig.set(path + ".level", md.getLevel());
            minesConfig.set(path + ".depth", md.getDepth());
        }
        // Players.
        for (PlayerData pd : players.values()) {
            String path = "players." + pd.getUuid().toString();
            playersConfig.set(path + ".name", pd.getName());
            playersConfig.set(path + ".balance", pd.getBalance());
            playersConfig.set(path + ".mineLevel", pd.getMineLevel());
            playersConfig.set(path + ".mineDepth", pd.getMineDepth());
            playersConfig.set(path + ".pickaxe", pd.getPickaxe().name());
            playersConfig.set(path + ".mineKey", pd.getMineKey());
            playersConfig.set(path + ".workingForMine", pd.getWorkingForMine());
            java.util.List<String> workers = new java.util.ArrayList<>();
            for (UUID w : pd.getWorkers()) {
                workers.add(w.toString());
            }
            playersConfig.set(path + ".workers", workers);
        }
        try {
            minesConfig.save(minesFile);
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data files", e);
        }
    }

    // ----- accessors -----

    public PlayerData getOrCreate(UUID uuid, String name) {
        PlayerData pd = players.get(uuid);
        if (pd == null) {
            pd = new PlayerData(uuid, name);
            pd.setBalance(plugin.getConfigManager().startingBalance());
            players.put(uuid, pd);
        } else if (name != null && !name.equals(pd.getName())) {
            pd.setName(name);
        }
        return pd;
    }

    public PlayerData get(UUID uuid) {
        return players.get(uuid);
    }

    public MineData getMine(String key) {
        return mines.get(key);
    }

    public MineData getMineByOwner(UUID owner) {
        for (MineData md : mines.values()) {
            if (md.isOwner(owner)) return md;
        }
        return null;
    }

    public MineData getMineAt(int x, int y, int z) {
        for (MineData md : mines.values()) {
            if (md.isInsideShaft(x, y, z) || md.isWall(x, y, z)) {
                return md;
            }
        }
        return null;
    }

    public boolean hasAccess(UUID uuid, MineData md) {
        if (md == null) return false;
        if (md.isOwner(uuid)) return true;
        PlayerData owner = players.get(md.getOwner());
        if (owner != null && owner.getWorkers().contains(uuid)) return true;
        PlayerData worker = players.get(uuid);
        if (worker != null && md.getKey().equals(worker.getWorkingForMine())) return true;
        return false;
    }

    /**
     * Allocates a brand-new mine for a player at the next free coordinate slot.
     */
    public MineData createMineFor(UUID uuid, String name) {
        if (getMineByOwner(uuid) != null) {
            return getMineByOwner(uuid);
        }
        PlayerData pd = getOrCreate(uuid, name);
        if (pd.getMineKey() != null && mines.containsKey(pd.getMineKey())) {
            return mines.get(pd.getMineKey());
        }

        int index = nextMineIndex++;
        String key = "mine_" + index;
        int spacingX = plugin.getConfigManager().spacingX();
        int spacingZ = plugin.getConfigManager().spacingZ();
        int surfaceY = plugin.getConfigManager().surfaceY();
        // Slot position. Use index from 1; first mine at index 1 sits at X=0.
        int x = (index - 1) * spacingX;
        int z = (index - 1) * spacingZ;
        int depth = plugin.getConfigManager().startDepth();
        MineData md = new MineData(key, uuid, x, surfaceY, z, 1, depth);
        mines.put(key, md);

        pd.setMineKey(key);
        pd.setMineLevel(1);
        pd.setMineDepth(depth);
        pd.setPickaxe(com.example.deepdigger.models.PickaxeType.WOOD);

        buildShaft(md);
        return md;
    }

    /**
     * Builds the physical shaft of a mine: 1-block column from surface down to
     * surfaceY - depth, with wall blocks on each side.
     */
    public void buildShaft(MineData md) {
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        if (w == null) {
            plugin.getLogger().warning("World not found: " + plugin.getConfigManager().worldName());
            return;
        }
        Material wall = matchMaterial(plugin.getConfigManager().wallBlock(), Material.SMOOTH_STONE);
        Material surf = matchMaterial(plugin.getConfigManager().surfaceBlock(), Material.GRASS_BLOCK);
        int x = md.getSurfaceX();
        int z = md.getSurfaceZ();
        int top = md.getSurfaceY();
        int bottom = md.getBottomY();

        // Build walls down to bottom.
        for (int y = top; y >= bottom; y--) {
            placeWallIfAir(w, x + 1, y, z, wall);
            placeWallIfAir(w, x - 1, y, z, wall);
            placeWallIfAir(w, x, y, z + 1, wall);
            placeWallIfAir(w, x, y, z - 1, wall);
        }
        // Place surface cap on top.
        w.getBlockAt(x, top, z).setType(surf, false);
        // Fill the shaft column with stone; this will be regenerated into ore
        // dynamically when mined.
        for (int y = top - 1; y >= bottom; y--) {
            w.getBlockAt(x, y, z).setType(Material.STONE, false);
        }
        // Place a small platform around the entrance so the player doesn't fall off.
        for (int dy = -1; dy <= 1; dy++) {
            w.getBlockAt(x + 1, top + dy, z).setType(Material.SMOOTH_STONE, false);
            w.getBlockAt(x - 1, top + dy, z).setType(Material.SMOOTH_STONE, false);
            w.getBlockAt(x, top + dy, z + 1).setType(Material.SMOOTH_STONE, false);
            w.getBlockAt(x, top + dy, z - 1).setType(Material.SMOOTH_STONE, false);
        }
    }

    private void placeWallIfAir(World w, int x, int y, int z, Material mat) {
        Block b = w.getBlockAt(x, y, z);
        if (b.getType() == Material.AIR) {
            b.setType(mat, false);
        }
    }

    private Material matchMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material m = Material.matchMaterial(name);
        return m == null ? fallback : m;
    }

    /**
     * Extends the shaft when the mine is upgraded. Only builds the new
     * lower portion so existing structures aren't disturbed.
     */
    public void extendShaft(MineData md, int oldDepth, int newDepth) {
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        if (w == null) return;
        Material wall = matchMaterial(plugin.getConfigManager().wallBlock(), Material.SMOOTH_STONE);
        int x = md.getSurfaceX();
        int z = md.getSurfaceZ();
        int oldBottom = md.getSurfaceY() - oldDepth;
        int newBottom = md.getSurfaceY() - newDepth;
        for (int y = oldBottom - 1; y >= newBottom; y--) {
            placeWallIfAir(w, x + 1, y, z, wall);
            placeWallIfAir(w, x - 1, y, z, wall);
            placeWallIfAir(w, x, y, z + 1, wall);
            placeWallIfAir(w, x, y, z - 1, wall);
            w.getBlockAt(x, y, z).setType(Material.STONE, false);
        }
    }

    /**
     * Get a depth-aware ore for a freshly regenerated block in the shaft.
     */
    public Material pickOreForDepth(int depth) {
        for (Map<String, Object> ore : plugin.getConfigManager().oreList()) {
            int min = (int) ore.get("min-depth");
            int max = (int) ore.get("max-depth");
            if (depth < min || depth > max) continue;
            double chance = (double) ore.get("chance");
            if (chance >= 1.0) {
                return matchMaterial((String) ore.get("block"), Material.STONE);
            }
            if (Math.random() < chance) {
                return matchMaterial((String) ore.get("block"), Material.STONE);
            }
        }
        return Material.STONE;
    }

    public Map<String, MineData> all() {
        return mines;
    }

    public Map<UUID, PlayerData> players() {
        return players;
    }

    /**
     * Reset player's mine: keep UUID/balance, reset shaft level/depth to defaults.
     */
    public void resetMine(UUID uuid) {
        PlayerData pd = get(uuid);
        if (pd == null) return;
        pd.setMineLevel(1);
        pd.setMineDepth(plugin.getConfigManager().startDepth());
        MineData md = getMineByOwner(uuid);
        if (md != null) {
            int oldDepth = md.getDepth();
            int newDepth = plugin.getConfigManager().startDepth();
            md.setLevel(1);
            md.setDepth(newDepth);
            // Rebuild the shaft to fresh state. Easiest: build the whole shaft again
            // overwriting contents.
            buildShaft(md);
        }
    }

    /**
     * Fully delete a player's mine and free the slot.
     */
    public void deleteMine(UUID uuid) {
        PlayerData pd = get(uuid);
        if (pd == null) return;
        String key = pd.getMineKey();
        if (key == null) return;
        MineData md = mines.remove(key);
        pd.setMineKey(null);
        pd.setMineLevel(1);
        pd.setMineDepth(plugin.getConfigManager().startDepth());
        pd.getWorkers().clear();
        pd.setWorkingForMine(null);
        if (md != null) {
            // Clear out the physical shaft too.
            World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
            if (w != null) {
                int x = md.getSurfaceX();
                int z = md.getSurfaceZ();
                int top = md.getSurfaceY();
                int bottom = md.getBottomY();
                for (int y = top + 1; y >= bottom - 2; y--) {
                    w.getBlockAt(x, y, z).setType(Material.AIR, false);
                    w.getBlockAt(x + 1, y, z).setType(Material.AIR, false);
                    w.getBlockAt(x - 1, y, z).setType(Material.AIR, false);
                    w.getBlockAt(x, y, z + 1).setType(Material.AIR, false);
                    w.getBlockAt(x, y, z - 1).setType(Material.AIR, false);
                }
            }
        }
        // Also clear from YAML config.
        if (minesConfig != null) minesConfig.set("mines." + key, null);
    }

    public Location surfaceLocation(MineData md) {
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        return new Location(w, md.getSurfaceX() + 0.5, md.getSurfaceY() + 1, md.getSurfaceZ() + 0.5);
    }
}
