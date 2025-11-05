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

                    @SuppressWarnings("unchecked")
                    Map<String, Object> entry = (Map<String, Object>) obj;

                    try {
                        String name = String.valueOf(entry.get("name"));
                        String level = String.valueOf(entry.get("level"));

                        @SuppressWarnings("unchecked")
                        Map<String, Object> p1m = (Map<String, Object>) entry.get("p1");

                        @SuppressWarnings("unchecked")
                        Map<String, Object> p2m = (Map<String, Object>) entry.get("p2");

                        Map<String, Boolean> flags = new HashMap<>();
                        Object flagsObj = entry.get("flags");
                        if (flagsObj instanceof Map) {
                            @SuppressWarnings("unchecked")
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
        return r != null && r.getFlag("safe", false);
    }

    public boolean isPvP(Location loc) {
        Region r = getRegionAt(loc);
        if (r == null) return false;
        if (r.getFlag("safe", false)) return false;
        return r.getFlag("pvp", false);
    }

    public boolean canBuild(Location loc) {
        Region r = getRegionAt(loc);
        if (r == null) return true; // allow building outside regions
        return r.getFlag("build", true); // allow inside regions unless build:false
    }


    public boolean allowMobSpawn(Location loc) {
        Region r = getRegionAt(loc);
        return r == null || r.getFlag("mobspawn", true);
    }

    public void reload() {
        load();
    }
}
