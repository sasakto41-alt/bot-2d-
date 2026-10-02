package com.example.deepdigger.models;

/**
 * Catalog of pickaxe tiers used both for required-tier checks and for
 * the buyable pickaxe catalog. The integer ordinal grows with tier power
 * so simple comparison ("this tier >= required tier") is enough.
 */
public enum PickaxeType {
    WOOD,
    STONE,
    IRON,
    DIAMOND;

    public int tier() {
        return ordinal() + 1;
    }

    public String roman() {
        switch (this) {
            case WOOD: return "I";
            case STONE: return "II";
            case IRON: return "III";
            case DIAMOND: return "IV";
            default: return "?";
        }
    }

    public static PickaxeType fromRoman(String s) {
        if (s == null) return null;
        switch (s.trim()) {
            case "I": return WOOD;
            case "II": return STONE;
            case "III": return IRON;
            case "IV": return DIAMOND;
            default: return null;
        }
    }
}
