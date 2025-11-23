package com.zombielooter.zones;

import cn.nukkit.level.Location;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

public class ZoneManager {

    private final ZombieLooterX plugin;
    private final List<Region> regions = new ArrayList<>();
    private Config zonesConfig;

    public ZoneManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
    }

    public final void load() {
        try {
            File f = new File(plugin.getDataFolder(), "zones.yml");
            if (!f.exists()) plugin.saveResource("zones.yml", false);

            zonesConfig = new Config(f, Config.YAML);
            regions.clear();

            List<?> rawList = zonesConfig.getList("regions");
            if (rawList != null) {
                for (Object obj : rawList) {
                    if (!(obj instanceof Map)) continue;

                    Map<String, Object> entry = (Map<String, Object>) obj;

                    try {
                        String name = String.valueOf(entry.get("name"));
                        String level = String.valueOf(entry.get("level"));

                        Map<String, Object> p1m = (Map<String, Object>) entry.get("p1");

                        Map<String, Object> p2m = (Map<String, Object>) entry.get("p2");

                        Map<String, Boolean> flags = new HashMap<>();
                        Object flagsObj = entry.get("flags");
                        if (flagsObj instanceof Map) {
                            Map<String, Object> flagsRaw = (Map<String, Object>) flagsObj;
                            for (Map.Entry<String, Object> fe : flagsRaw.entrySet()) {
                                flags.put(fe.getKey().toLowerCase(Locale.ROOT), Boolean.TRUE.equals(fe.getValue()));
                            }
                        }

                        Vector3 p1 = new Vector3(toDouble(p1m.get("x")), toDouble(p1m.get("y")), toDouble(p1m.get("z")));
                        Vector3 p2 = new Vector3(toDouble(p2m.get("x")), toDouble(p2m.get("y")), toDouble(p2m.get("z")));

                        regions.add(new Region(name, level, p1, p2, flags));
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Invalid region entry in zones.yml: " + ex.getMessage());
                    }
                }
            }

            plugin.getLogger().info("Zones loaded: " + regions.size());
        } catch (Exception ex) {
            plugin.getLogger().error("Failed to load zones.yml: " + ex.getMessage());
        }
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public Region getRegionAt(Location loc) {
        for (Region r : regions) {
            if (r.contains(loc)) return r;
        }
        return null;
    }

    public boolean isSafe(Location loc) {
        Region r = getRegionAt(loc);
        return r != null && r.getFlag(Region.FLAG_SAFE, false);
    }

    public boolean isPvP(Location loc) {
        Region r = getRegionAt(loc);
        if (r == null) return false;
        if (r.getFlag(Region.FLAG_SAFE, false)) return false;
        return r.getFlag(Region.FLAG_PVP, false);
    }

    public boolean canBuild(Location loc) {
        Region r = getRegionAt(loc);
        if (r == null) return true; // allow building outside regions
        return r.getFlag(Region.FLAG_BUILD, true); // allow inside regions unless build:false
    }


    public boolean allowMobSpawn(Location loc) {
        Region r = getRegionAt(loc);
        return r == null || r.getFlag(Region.FLAG_MOBSPAWN, true);
    }

    /**
     * Updates a region corner in zones.yml to the player's current location (block-aligned).
     * Creates the region with default flags if it does not exist yet.
     *
     * @param regionName name of the region to update or create
     * @param loc        player location to capture (must include level)
     * @param corner     "p1" or "p2"
     * @return true if saved and reloaded successfully, false otherwise
     */
    public synchronized boolean setRegionCorner(String regionName, Location loc, String corner) {
        if (zonesConfig == null || regionName == null || loc == null || loc.getLevel() == null) return false;

        String normalized = corner == null ? "" : corner.toLowerCase(Locale.ROOT);
        if (!normalized.equals("p1") && !normalized.equals("p2")) return false;

        try {
            List<Object> regionsList = new ArrayList<>(zonesConfig.getList("regions", new ArrayList<>()));
            Map<String, Object> target = null;

            for (int i = 0; i < regionsList.size(); i++) {
                Object o = regionsList.get(i);
                if (o instanceof Map) {
                    Map<String, Object> map = new LinkedHashMap<>((Map<String, Object>) o);
                    if (regionName.equalsIgnoreCase(String.valueOf(map.get("name")))) {
                        target = map;
                        regionsList.set(i, target);
                        break;
                    }
                }
            }

            if (target == null) {
                target = new LinkedHashMap<>();
                target.put("name", regionName);
                target.put("level", loc.getLevel().getName());
                target.put("p1", pointFromLocation(loc));
                target.put("p2", pointFromLocation(loc));

                Map<String, Boolean> defaults = new LinkedHashMap<>();
                defaults.put(Region.FLAG_SAFE, false);
                defaults.put(Region.FLAG_PVP, false);
                defaults.put(Region.FLAG_BUILD, false);
                defaults.put(Region.FLAG_MOBSPAWN, true);
                target.put("flags", defaults);

                regionsList.add(target);
            }

            target.put("level", loc.getLevel().getName());
            target.put(normalized, pointFromLocation(loc));

            zonesConfig.set("regions", regionsList);
            zonesConfig.save();
            load(); // refresh in-memory regions
            return true;
        } catch (Exception ex) {
            plugin.getLogger().error("Failed to save region corner: " + ex.getMessage());
            return false;
        }
    }

    private Map<String, Object> pointFromLocation(Location loc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", loc.getFloorX());
        map.put("y", loc.getFloorY());
        map.put("z", loc.getFloorZ());
        return map;
    }

    public void reload() {
        load();
    }
}
