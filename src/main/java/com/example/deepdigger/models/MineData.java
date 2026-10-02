package com.example.deepdigger.models;

import java.util.UUID;

/**
 * Coordinates and metadata for a single personal mine.
 * <p>
 * A mine is now a 3×3 vertical shaft surrounded by glass walls (5×5 outer
 * footprint) that floats on a platform in the sky. The shaft column is
 * (x in [cx-1..cx+1], z in [cz-1..cz+1]), and y in [bottomY..surfaceY].
 * The bedrock floor sits at y = bottomY - 1 across the same 3×3 area.
 * Glass walls surround the shaft on the 4 sides at the same X-Z ring
 * offset (one block out from the shaft in each cardinal direction).
 */
public class MineData {

    private String key;
    private UUID owner;
    private int surfaceX;
    private int surfaceY;
    private int surfaceZ;
    private int level;
    private int depth;
    private int shaftWidth = 3; // half-width on each side = (width-1)/2

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

    public int getShaftWidth() { return shaftWidth; }
    public void setShaftWidth(int w) { this.shaftWidth = w; }

    public int halfWidth() { return (shaftWidth - 1) / 2; }

    /** Lowest Y of the mine (bedrock floor is one below this). */
    public int getBottomY() { return surfaceY - depth; }

    /** True if a block is inside the 3×3 shaft column itself. */
    public boolean isInsideShaft(int x, int y, int z) {
        int h = halfWidth();
        if (Math.abs(x - surfaceX) > h) return false;
        if (Math.abs(z - surfaceZ) > h) return false;
        return y >= getBottomY() && y <= surfaceY;
    }

    /** True if the block is on the glass walls surrounding the shaft. */
    public boolean isWall(int x, int y, int z) {
        if (y < getBottomY() || y > surfaceY) return false;
        int h = halfWidth();
        int dx = Math.abs(x - surfaceX);
        int dz = Math.abs(z - surfaceZ);
        // Wall ring: any block whose outer footprint ring touches the shaft
        // perimeter at h+1.
        if (Math.max(dx, dz) != h + 1) return false;
        // The ring corners (dx==h+1 && dz==h+1) also count as walls.
        return true;
    }

    /** True if the block is the unbreakable bedrock floor under the shaft. */
    public boolean isBottom(int x, int y, int z) {
        int h = halfWidth();
        if (Math.abs(x - surfaceX) > h) return false;
        if (Math.abs(z - surfaceZ) > h) return false;
        return y == getBottomY() - 1;
    }

    /** Depth at a given Y inside the shaft (0 at surface). */
    public int depthAt(int y) {
        return surfaceY - y;
    }

    public boolean isOwner(UUID uuid) {
        return owner != null && owner.equals(uuid);
    }
}
