package com.zombielooter.boss;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.mob.EntityZombie;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

/**
 * BossEventManager - Handles loading and spawning of boss entities.
 * Each boss entry in bosses.yml defines:
 *
 * bosses:
 *   - id: "undead_king"
 *     name: "Undead King"
 *     type: "zombie"
 *     health: 400
 *     damage: 8
 */
public class BossEventManager {

    private final ZombieLooterX plugin;
    private final Map<String, Map<String, Object>> bosses = new HashMap<>();

    public BossEventManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Loads boss configurations from bosses.yml.
     */
    public final void load() {
        try {
            File file = new File(plugin.getDataFolder(), "bosses.yml");
            if (!file.exists()) {
                plugin.saveResource("bosses.yml", false);
            }

            Config config = new Config(file, Config.YAML);
            bosses.clear();

            List<?> rawList = config.getList("bosses");
            if (rawList == null || rawList.isEmpty()) {
                plugin.getLogger().warning("⚠ No bosses found in bosses.yml");
                return;
            }

            for (Object obj : rawList) {
                if (!(obj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) obj;

                String id = String.valueOf(data.getOrDefault("id", UUID.randomUUID().toString()));
                bosses.put(id, data);
            }

            plugin.getLogger().info("✅ Loaded bosses: " + bosses.size());
        } catch (Exception ex) {
            plugin.getLogger().error("❌ Failed to load bosses.yml: " + ex.getMessage());
        }
    }

    /**
     * Triggers a boss spawn by ID.
     */
    public boolean trigger(String id) {
        Map<String, Object> data = bosses.get(id);
        if (data == null) {
            plugin.getLogger().warning("⚠ Boss '" + id + "' not found.");
            return false;
        }

        try {
            String type = String.valueOf(data.getOrDefault("type", "zombie"));
            String name = String.valueOf(data.getOrDefault("name", "Unknown Boss"));
            int health = ((Number) data.getOrDefault("health", 200)).intValue();

            Level level = plugin.getServer().getDefaultLevel();
            if (level == null) {
                plugin.getLogger().error("⚠ No default level loaded to spawn boss!");
                return false;
            }

            Location spawnLoc = level.getSafeSpawn().getLocation();
            Entity entity = Entity.createEntity(type, spawnLoc);

            if (entity == null) {
                plugin.getLogger().warning("⚠ Could not create entity type: " + type);
                return false;
            }

            entity.setNameTag("§c" + name);
            entity.setNameTagVisible(true);

            if (entity instanceof EntityZombie) {
                ((EntityZombie) entity).setHealth(health);
            }

            entity.spawnToAll();
            plugin.getLogger().info("👑 Spawned boss: " + name + " (" + type + ")");
            return true;
        } catch (Exception ex) {
            plugin.getLogger().error("⚠ Failed to spawn boss '" + id + "': " + ex.getMessage());
            return false;
        }
    }

    /**
     * Lists all loaded boss IDs.
     */
    public Set<String> listBosses() {
        return bosses.keySet();
    }

    /**
     * Reload bosses.yml.
     */
    public void reload() {
        load();
    }
}
