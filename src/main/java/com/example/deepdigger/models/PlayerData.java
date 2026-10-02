package com.example.deepdigger.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent per-player state. Stored in players.yml under the player's UUID.
 * <p>
 * Fields are intentionally simple primitives so YAML (de)serialization
 * stays straightforward. The fields are mutated in place by managers and
 * flushed by the save task.
 */
public class PlayerData {

    private UUID uuid;
    private String name;
    private double balance;
    private int mineLevel;
    private int mineDepth;
    private PickaxeType pickaxe;
    private String mineKey;          // logical key of the mine (e.g. "mine_3")
    private List<UUID> workers;      // workers of THIS player's mine
    private String workingForMine;  // if non-null, this player works in someone else's mine

    public PlayerData() {
        this.balance = 0.0;
        this.mineLevel = 1;
        this.mineDepth = 30;
        this.pickaxe = PickaxeType.WOOD;
        this.workers = new ArrayList<>();
    }

    public PlayerData(UUID uuid, String name) {
        this();
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public int getMineLevel() { return mineLevel; }
    public void setMineLevel(int mineLevel) { this.mineLevel = mineLevel; }

    public int getMineDepth() { return mineDepth; }
    public void setMineDepth(int mineDepth) { this.mineDepth = mineDepth; }

    public PickaxeType getPickaxe() { return pickaxe; }
    public void setPickaxe(PickaxeType pickaxe) { this.pickaxe = pickaxe; }

    public String getMineKey() { return mineKey; }
    public void setMineKey(String mineKey) { this.mineKey = mineKey; }

    public List<UUID> getWorkers() { return workers; }
    public void setWorkers(List<UUID> workers) { this.workers = workers; }

    public String getWorkingForMine() { return workingForMine; }
    public void setWorkingForMine(String workingForMine) { this.workingForMine = workingForMine; }

    public boolean isWorker() { return workingForMine != null; }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid + ", name=" + name + ", balance=" + balance
                + ", mineLevel=" + mineLevel + ", mineDepth=" + mineDepth
                + ", pickaxe=" + pickaxe + ", mineKey=" + mineKey
                + ", workers=" + workers.size()
                + ", workingForMine=" + workingForMine + "}";
    }
}
