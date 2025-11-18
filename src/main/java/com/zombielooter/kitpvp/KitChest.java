package com.zombielooter.kitpvp;

import cn.nukkit.level.Location;

/**
 * Stores the position of a kit selection chest and hologram offset.
 */
public class KitChest {
    private final String kitId;
    private final Location location;
    private final double hologramOffset;

    public KitChest(String kitId, Location location, double hologramOffset) {
        this.kitId = kitId;
        this.location = location;
        this.hologramOffset = hologramOffset;
    }

    public String getKitId() {
        return kitId;
    }

    public Location getLocation() {
        return location;
    }

    public double getHologramOffset() {
        return hologramOffset;
    }
}
