package com.zombielooter.zones;

import cn.nukkit.level.Location;
import cn.nukkit.math.Vector3;
import java.util.HashMap;
import java.util.Map;

public class Region {
    private final String name;
    private final String levelName;
    private final Vector3 min;
    private final Vector3 max;
    private final Map<String, Boolean> flags = new HashMap<>();

    public Region(String name, String levelName, Vector3 p1, Vector3 p2, Map<String, Boolean> initialFlags) {
        this.name = name;
        this.levelName = levelName;
        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxX = Math.max(p1.getX(), p2.getX());
        double maxY = Math.max(p1.getY(), p2.getY());
        double maxZ = Math.max(p1.getZ(), p2.getZ());
        this.min = new Vector3(minX, minY, minZ);
        this.max = new Vector3(maxX, maxY, maxZ);
        flags.put("safe", false);
        flags.put("pvp", false);
        flags.put("build", false);
        flags.put("mobspawn", true);
        if (initialFlags != null) {
            for (Map.Entry<String, Boolean> e : initialFlags.entrySet()) {
                flags.put(e.getKey().toLowerCase(), e.getValue());
            }
        }
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getLevel() == null) return false;
        if (!loc.getLevel().getName().equalsIgnoreCase(levelName)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= min.getX() && x <= max.getX() && y >= min.getY() && y <= max.getY() && z >= min.getZ() && z <= max.getZ();
    }

    public boolean getFlag(String key, boolean def) { return flags.getOrDefault(key.toLowerCase(), def); }
    public String getName() { return name; }
    public String getLevelName() { return levelName; }
    public Vector3 getMin() { return min; }
    public Vector3 getMax() { return max; }
}
