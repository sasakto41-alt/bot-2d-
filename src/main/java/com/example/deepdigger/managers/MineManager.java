package com.example.deepdigger.managers;

import com.example.deepdigger.DeepDiggerPlugin;
import com.example.deepdigger.models.MineData;
import com.example.deepdigger.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
 * Every player gets exactly one mine. The mine sits on a floating
 * platform in the sky (default Y=120). The shaft is 3x3 and is
 * surrounded by glass walls so it visually reads as a 2D corridor.
 * A bedrock floor caps the bottom so the player can never dig past it.
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
                md.setShaftWidth(m.getInt("shaftWidth", 3));
                mines.put(key, md);
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
        for (MineData md : mines.values()) {
            String path = "mines." + md.getKey();
            minesConfig.set(path + ".owner", md.getOwner() == null ? null : md.getOwner().toString());
            minesConfig.set(path + ".surfaceX", md.getSurfaceX());
            minesConfig.set(path + ".surfaceY", md.getSurfaceY());
            minesConfig.set(path + ".surfaceZ", md.getSurfaceZ());
            minesConfig.set(path + ".level", md.getLevel());
            minesConfig.set(path + ".depth", md.getDepth());
            minesConfig.set(path + ".shaftWidth", md.getShaftWidth());
        }
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
            if (md.isInsideShaft(x, y, z) || md.isWall(x, y, z) || md.isBottom(x, y, z)) {
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
        int x = (index - 1) * spacingX;
        int z = (index - 1) * spacingZ;
        int depth = plugin.getConfigManager().startDepth();
        MineData md = new MineData(key, uuid, x, surfaceY, z, 1, depth);
        md.setShaftWidth(plugin.getConfigManager().shaftWidth());
        mines.put(key, md);

        pd.setMineKey(key);
        pd.setMineLevel(1);
        pd.setMineDepth(depth);
        pd.setPickaxe(com.example.deepdigger.models.PickaxeType.WOOD);

        buildShaft(md);
        return md;
    }

    /**
     * Builds the physical shaft of a mine: a 3x3 (configurable) column from
     * the surface down to surfaceY - depth, with glass walls around the
     * perimeter, a bedrock floor at the bottom, and a 7x7 platform on top
     * so the player has somewhere to stand on.
     */
    public void buildShaft(MineData md) {
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        if (w == null) {
            plugin.getLogger().warning("World not found: " + plugin.getConfigManager().worldName());
            return;
        }
        Material wall = matchMaterial(plugin.getConfigManager().wallBlock(), Material.GLASS);
        Material surf = matchMaterial(plugin.getConfigManager().surfaceBlock(), Material.GRASS_BLOCK);
        Material floor = matchMaterial(plugin.getConfigManager().bottomBlock(), Material.BEDROCK);
        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int h = md.halfWidth();
        int top = md.getSurfaceY();
        int bottom = md.getBottomY();

        // Place bedrock floor first (unbreakable bottom).
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                w.getBlockAt(cx + dx, bottom - 1, cz + dz).setType(floor, false);
            }
        }
        // Fill the shaft column with stone; this will be regenerated into ore
        // dynamically when mined.
        for (int y = top - 1; y >= bottom; y--) {
            for (int dx = -h; dx <= h; dx++) {
                for (int dz = -h; dz <= h; dz++) {
                    w.getBlockAt(cx + dx, y, cz + dz).setType(Material.STONE, false);
                }
            }
        }
        // Build glass walls around the perimeter (outer ring at h+1).
        for (int y = top; y >= bottom - 1; y--) {
            for (int i = -(h + 1); i <= h + 1; i++) {
                // Four sides of the ring.
                placeWallIfAir(w, cx + (h + 1), y, cz + i, wall);
                placeWallIfAir(w, cx - (h + 1), y, cz + i, wall);
                placeWallIfAir(w, cx + i, y, cz + (h + 1), wall);
                placeWallIfAir(w, cx + i, y, cz - (h + 1), wall);
            }
        }
        // Surface cap on top of the shaft (player stands here).
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                w.getBlockAt(cx + dx, top, cz + dz).setType(surf, false);
            }
        }
        // Open the surface so player can dig down: clear the center 3x3 below the cap.
        // Wait, the cap is AT top, and the shaft starts BELOW the cap. That's fine.

        // Place a wider platform around the top so the player doesn't fall off
        // when they exit.
        int platformRadius = h + 1;
        for (int dx = -platformRadius; dx <= platformRadius; dx++) {
            for (int dz = -platformRadius; dz <= platformRadius; dz++) {
                if (Math.abs(dx) <= h && Math.abs(dz) <= h) {
                    // Already shaft top — skip.
                    continue;
                }
                // Only place a ring around the top.
                Block b = w.getBlockAt(cx + dx, top, cz + dz);
                if (b.getType() == Material.AIR) {
                    b.setType(Material.SMOOTH_STONE, false);
                }
            }
        }
        // Place a fence ring around the platform top so player doesn't fall off.
        // Two blocks tall so they can't jump over.
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -platformRadius - 1; dx <= platformRadius + 1; dx++) {
                for (int dz = -platformRadius - 1; dz <= platformRadius + 1; dz++) {
                    if (Math.abs(dx) != platformRadius + 1 && Math.abs(dz) != platformRadius + 1) continue;
                    Block b = w.getBlockAt(cx + dx, top + 1 + dy, cz + dz);
                    if (b.getType() == Material.AIR) {
                        b.setType(Material.SPRUCE_FENCE, false);
                    }
                }
            }
        }

        // Spawn holograms on top of the fence (player can right-click them).
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().spawnFor(md);
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
     * Extends the shaft when the mine is upgraded. Builds the new lower
     * portion (more stone, more walls, and a new bedrock floor at the new
     * bottom — replacing the old one so the player can dig further).
     */
    public void extendShaft(MineData md, int oldDepth, int newDepth) {
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        if (w == null) return;
        Material wall = matchMaterial(plugin.getConfigManager().wallBlock(), Material.GLASS);
        Material floor = matchMaterial(plugin.getConfigManager().bottomBlock(), Material.BEDROCK);
        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int h = md.halfWidth();
        int oldBottom = md.getSurfaceY() - oldDepth;
        int newBottom = md.getSurfaceY() - newDepth;

        // Clear the old bedrock floor at oldBottom - 1 and replace with stone
        // so the player can dig through it (it's no longer the bottom).
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                Block oldFloor = w.getBlockAt(cx + dx, oldBottom - 1, cz + dz);
                if (oldFloor.getType() == floor) {
                    oldFloor.setType(Material.STONE, false);
                }
            }
        }

        // Build new shaft section from oldBottom - 1 down to newBottom.
        for (int y = oldBottom - 1; y >= newBottom; y--) {
            for (int dx = -h; dx <= h; dx++) {
                for (int dz = -h; dz <= h; dz++) {
                    w.getBlockAt(cx + dx, y, cz + dz).setType(Material.STONE, false);
                }
            }
        }

        // Build glass walls on the new section.
        for (int y = oldBottom - 1; y >= newBottom - 1; y--) {
            for (int i = -(h + 1); i <= h + 1; i++) {
                placeWallIfAir(w, cx + (h + 1), y, cz + i, wall);
                placeWallIfAir(w, cx - (h + 1), y, cz + i, wall);
                placeWallIfAir(w, cx + i, y, cz + (h + 1), wall);
                placeWallIfAir(w, cx + i, y, cz - (h + 1), wall);
            }
        }

        // Place a new bedrock floor at newBottom - 1.
        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                w.getBlockAt(cx + dx, newBottom - 1, cz + dz).setType(floor, false);
            }
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
            md.setLevel(1);
            md.setDepth(plugin.getConfigManager().startDepth());
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
            // Remove holograms first.
            if (plugin.getHologramManager() != null) {
                plugin.getHologramManager().removeAllFor(md);
            }
            World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
            if (w != null) {
                int cx = md.getSurfaceX();
                int cz = md.getSurfaceZ();
                int h = md.halfWidth();
                int top = md.getSurfaceY();
                int bottom = md.getBottomY();
                // Clear shaft, walls, and platform.
                for (int y = top + 2; y >= bottom - 2; y--) {
                    for (int dx = -(h + 2); dx <= h + 2; dx++) {
                        for (int dz = -(h + 2); dz <= h + 2; dz++) {
                            w.getBlockAt(cx + dx, y, cz + dz).setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
        if (minesConfig != null) minesConfig.set("mines." + key, null);
    }

    public Location surfaceLocation(MineData md) {
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        return new Location(w, md.getSurfaceX() + 0.5, md.getSurfaceY() + 1, md.getSurfaceZ() + 0.5);
    }

    /**
     * Instantly regenerates every block in the shaft: every block from the
     * surface down to the bottom becomes either stone or ore based on its
     * depth. Pending regen entries for blocks in the shaft are cancelled.
     * Triggered by the "Обновить шахту" hologram.
     */
    public void refreshMine(MineData md) {
        if (md == null) return;
        World w = Bukkit.getWorld(plugin.getConfigManager().worldName());
        if (w == null) return;
        int cx = md.getSurfaceX();
        int cz = md.getSurfaceZ();
        int h = md.halfWidth();
        int top = md.getSurfaceY();
        int bottom = md.getBottomY();
        // Iterate every block in the shaft column.
        for (int y = top - 1; y >= bottom; y--) {
            for (int dx = -h; dx <= h; dx++) {
                for (int dz = -h; dz <= h; dz++) {
                    Block b = w.getBlockAt(cx + dx, y, cz + dz);
                    int depth = md.depthAt(y);
                    if (depth < 0 || depth > md.getDepth()) continue;
                    // Clear pending regen for this block so the regen task
                    // doesn't fight with us over it.
                    plugin.getRegenManager().clearPending(b.getLocation());
                    Material newMat = pickOreForDepth(depth);
                    b.setType(newMat, false);
                }
            }
        }
    }
}
