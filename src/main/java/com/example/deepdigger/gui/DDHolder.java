package com.example.deepdigger.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder for all Deep Digger GUIs. Holding the menu type in the
 * holder lets the click listener dispatch to the right handler.
 */
public class DDHolder implements InventoryHolder {
    public enum Type { MAIN, PICKAXE, MINE, WORKERS, INFO, INVITE }
    private final Type type;
    private Inventory inventory;
    public DDHolder(Type type) { this.type = type; }
    public Type getType() { return type; }
    public void setInventory(Inventory inv) { this.inventory = inv; }
    @Override public Inventory getInventory() { return inventory; }
}
