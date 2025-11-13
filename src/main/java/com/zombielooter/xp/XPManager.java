package com.zombielooter.xp;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

public class XPManager {

    private final ZombieLooterX plugin;
    private final Config xpConfig;
    private final Map<UUID, Integer> playerXP = new HashMap<>();
    // Use a TreeMap for level requirements to keep them sorted
    private final NavigableMap<Integer, Integer> levelUpXP = new TreeMap<>();

    public XPManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.xpConfig = new Config(new File(plugin.getDataFolder(), "xp.yml"), Config.YAML);
        loadXPData();
        loadLevelUpRequirements();
    }

    private void loadXPData() {
        // ** THE FIX IS HERE **
        // Gracefully handle both String and Integer keys from the config.
        for (Map.Entry<String, Object> entry : xpConfig.getAll().entrySet()) {
            try {
                // We only care about player UUIDs for this map
                UUID uuid = UUID.fromString(entry.getKey());
                if (entry.getValue() instanceof Integer) {
                    playerXP.put(uuid, (Integer) entry.getValue());
                }
            } catch (IllegalArgumentException e) {
                // Ignore keys that are not valid UUIDs (like the 'level-requirements' section)
            }
        }
    }

    private void loadLevelUpRequirements() {
        levelUpXP.clear();
        // Load from the 'level-requirements' section of xp.yml
        if (xpConfig.exists("level-requirements")) {
            for (Map.Entry<String, Object> entry : xpConfig.getSection("level-requirements").getAll().entrySet()) {
                try {
                    int level = Integer.parseInt(entry.getKey());
                    int requiredXp = (Integer) entry.getValue();
                    levelUpXP.put(level, requiredXp);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level key in xp.yml: " + entry.getKey());
                }
            }
        }
        // Add a default if empty
        if (levelUpXP.isEmpty()) {
            levelUpXP.put(1, 0);
            levelUpXP.put(2, 1000);
        }
    }

    public void save() {
        // Clear the config to remove old data
        xpConfig.setAll(new LinkedHashMap<>());
        
        // Save player XP
        for (Map.Entry<UUID, Integer> entry : playerXP.entrySet()) {
            xpConfig.set(entry.getKey().toString(), entry.getValue());
        }

        // Save level requirements
        Map<String, Integer> levelsToSave = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : levelUpXP.entrySet()) {
            levelsToSave.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        xpConfig.set("level-requirements", levelsToSave);

        xpConfig.save();
    }

    public void addXP(Player player, int amount) {
        if (player == null) return;
        
        double boost = plugin.getTerritoryBuffManager().getXpBoost(player);
        int finalAmount = (int) (amount * (1 + (boost / 100.0)));

        int currentXP = getXP(player.getUniqueId());
        int newXP = currentXP + finalAmount;
        playerXP.put(player.getUniqueId(), newXP);

        player.sendMessage("§a+" + finalAmount + " XP" + (boost > 0 ? " (" + boost + "% Faction Boost)" : ""));

        int currentLevel = getLevel(player.getUniqueId());
        // Check if the new XP crosses a level-up threshold
        Map.Entry<Integer, Integer> nextLevelEntry = levelUpXP.higherEntry(currentLevel);
        if (nextLevelEntry != null && newXP >= nextLevelEntry.getValue()) {
            player.sendTitle("§b§lLEVEL UP!", "§eYou are now level " + nextLevelEntry.getKey());
        }
    }

    public int getXP(UUID uuid) {
        return playerXP.getOrDefault(uuid, 0);
    }

    public int getLevel(UUID uuid) {
        int xp = getXP(uuid);
        Map.Entry<Integer, Integer> entry = levelUpXP.floorEntry(xp);
        return (entry != null) ? entry.getKey() : 1;
    }
}
