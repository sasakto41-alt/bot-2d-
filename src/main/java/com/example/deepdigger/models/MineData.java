package com.example.deepdigger.models;

import java.util.UUID;

/**
 * Coordinates and metadata for a single personal mine.
 * <p>
 * A mine occupies a single 1x{depth} column at (x, surfaceY, z). Walls are
 * placed around the column on the same X-Z pair, from the surface down to
 * surfaceY - depth. The mine is keyed by a logical key like "mine_3" so
 * storage layout is decoupled from coordinate bookkeeping.
 */
public class MineData {

    private String key;
    private UUID owner;
    private int surfaceX;
    private int surfaceY;
    private int surfaceZ;
    private int level;
    private int depth;

    public MineData() {}

    public MineData(String key, UUID owner, int x, int y, int z, int level, int depth) {
        this.key = key;
        this.owner = owner;
        this.surfaceX = x;
        this.surfaceY = y;
        this.surfaceZ = z;
        this.level = level;
        this.depth = depth;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    public int getSurfaceX() { return surfaceX; }
    public void setSurfaceX(int surfaceX) { this.surfaceX = surfaceX; }

    public int getSurfaceY() { return surfaceY; }
    public void setSurfaceY(int surfaceY) { this.surfaceY = surfaceY; }

    public int getSurfaceZ() { return surfaceZ; }
    public void setSurfaceZ(int surfaceZ) { this.surfaceZ = surfaceZ; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    /** Lowest Y of the mine. */
    public int getBottomY() { return surfaceY - depth; }

    /** Returns true if a block at the given coordinates is inside the shaft column itself. */
    public boolean isInsideShaft(int x, int y, int z) {
        return x == surfaceX && z == surfaceZ
                && y >= getBottomY() && y <= surfaceY;
    }

    /** Returns true if the block is one of the four wall positions around the shaft at this y. */
    public boolean isWall(int x, int y, int z) {
        if (y < getBottomY() || y > surfaceY) return false;
        int dx = x - surfaceX;
        int dz = z - surfaceZ;
        return (Math.abs(dx) == 1 && dz == 0) || (Math.abs(dz) == 1 && dx == 0);
    }

    /** Returns the depth (0..depth) at a given Y inside the shaft. */
    public int depthAt(int y) {
        return surfaceY - y;
    }

    public boolean isOwner(UUID uuid) {
        return owner != null && owner.equals(uuid);
    }
}
