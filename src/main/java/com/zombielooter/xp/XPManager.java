package com.zombielooter.xp;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * XPManager keeps track of experience points and levels for each player.
 * XP thresholds and level rewards are defined in xp.yml.  Data is saved
 * asynchronously to xp_data.yml to avoid blocking the server thread.
 */
public class XPManager {
    private final ZombieLooterX plugin;
    private final Map<UUID, Integer> xp = new HashMap<>();
    private final Map<UUID, Integer> level = new HashMap<>();
    private Config dataConfig;

    public XPManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Load XP and level data from xp_data.yml asynchronously.
     */
    public void load() {
        CompletableFuture.runAsync(() -> {
            try {
                File f = new File(plugin.getDataFolder(), "xp_data.yml");
                if (!f.exists()) {
                    plugin.saveResource("xp_data.yml", false);
                }
                dataConfig = new Config(f, Config.YAML);
                for (String key : dataConfig.getKeys()) {
                    UUID id = UUID.fromString(key);
                    Map<String, Object> m = dataConfig.getSection(key).getAll();
                    xp.put(id, ((Number) m.getOrDefault("xp", 0)).intValue());
                    level.put(id, ((Number) m.getOrDefault("level", 1)).intValue());
                }
                plugin.getLogger().info("Loaded XP data: " + xp.size() + " players.");
            } catch (Exception e) {
                plugin.getLogger().error("Failed to load xp_data.yml: " + e.getMessage());
            }
        });
    }

    /**
     * Persist XP and level data back to xp_data.yml asynchronously.
     */
    public void save() {
        CompletableFuture.runAsync(() -> {
            Map<String, Object> out = new HashMap<>();
            xp.forEach((u, xpVal) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("xp", xpVal);
                m.put("level", level.getOrDefault(u, 1));
                out.put(u.toString(), m);
            });
            dataConfig.setAll((LinkedHashMap<String, Object>) out);
            dataConfig.save();
        });
    }

    public int getXP(UUID id) {
        return xp.getOrDefault(id, 0);
    }

    public int getLevel(UUID id) {
        return level.getOrDefault(id, 1);
    }

    /**
     * Add XP to a player and handle level ups.  Level thresholds and rewards
     * are defined in xp.yml.  On level up the player receives coins via
     * EconomyManager and a title is displayed.
     *
     * @param p      player
     * @param amount XP to add
     */
    public void addXP(Player p, int amount) {
        UUID id = p.getUniqueId();
        int curXp = getXP(id) + amount;
        int curLvl = getLevel(id);
        // process level ups
        while (curXp >= xpNeeded(curLvl)) {
            curXp -= xpNeeded(curLvl);
            curLvl++;
            rewardLevel(p, curLvl);
            p.sendTitle("§6Level Up!", "§eYou reached level " + curLvl, 5, 40, 5);
        }
        xp.put(id, curXp);
        level.put(id, curLvl);
        save();
    }

    /**
     * Compute the XP needed to reach a given level.  Uses a base and growth
     * factor defined in xp.yml.
     *
     * @param lvl current level
     * @return XP needed to reach level+1
     */
    private int xpNeeded(int lvl) {
        Config cfg = new Config(new File(plugin.getDataFolder(), "xp.yml"), Config.YAML);
        int base = cfg.getInt("base", 100);
        double growth = cfg.getDouble("growth", 1.5);
        return (int) (base * Math.pow(growth, lvl - 1));
    }

    /**
     * Reward the player for reaching a new level.  Reward amounts are read
     * from the xp.yml file under rewards.<level>.
     *
     * @param p   player
     * @param lvl new level
     */
    private void rewardLevel(Player p, int lvl) {
        Config cfg = new Config(new File(plugin.getDataFolder(), "xp.yml"), Config.YAML);
        int reward = cfg.getInt("rewards." + lvl, 0);
        if (reward > 0) {
            plugin.getEconomyManager().addBalance(p.getUniqueId(), reward);
            p.sendPopup("§a+" + reward + " coins!");
        }
    }
}