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
    // Use a TreeMap keyed by required XP to keep them sorted by threshold
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
            Map<String, Object> rawRequirements = xpConfig.getSection("level-requirements").getAll();
            boolean xpKeyed = "xp-keyed".equalsIgnoreCase(xpConfig.getString("level-requirements-format"));
            Set<Integer> keys = new HashSet<>();
            List<Map.Entry<Integer, Integer>> parsedEntries = new ArrayList<>();

            for (Map.Entry<String, Object> entry : rawRequirements.entrySet()) {
                try {
                    int parsedKey = Integer.parseInt(entry.getKey());
                    if (entry.getValue() instanceof Integer value) {
                        keys.add(parsedKey);
                        parsedEntries.add(new AbstractMap.SimpleEntry<>(parsedKey, value));
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level requirement key in xp.yml: " + entry.getKey());
                }
            }

            boolean legacyLevelKeyed = !xpKeyed && !rawRequirements.isEmpty() && isLikelyLevelKeyed(keys);

            for (Map.Entry<Integer, Integer> parsed : parsedEntries) {
                int key = parsed.getKey();
                int value = parsed.getValue();
                if (xpKeyed || (!legacyLevelKeyed && !xpKeyed)) {
                    levelUpXP.put(key, value);
                } else {
                    levelUpXP.put(value, key);
                }
            }
        }

        // Add a default if empty
        if (levelUpXP.isEmpty()) {
            levelUpXP.put(0, 1);
            levelUpXP.put(1000, 2);
        }
    }

    public void save() {
        // Clear the config to remove old data
        xpConfig.setAll(new LinkedHashMap<>());
        
        // Save player XP
        for (Map.Entry<UUID, Integer> entry : playerXP.entrySet()) {
            xpConfig.set(entry.getKey().toString(), entry.getValue());
        }

        // Save level requirements keyed by required XP
        Map<String, Integer> levelsToSave = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : levelUpXP.entrySet()) {
            levelsToSave.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        xpConfig.set("level-requirements", levelsToSave);
        xpConfig.set("level-requirements-format", "xp-keyed");

        xpConfig.save();
    }

    public void addXP(Player player, int amount) {
        if (player == null) return;
        
        double boost = plugin.getTerritoryBuffManager().getXpBoost(player);
        int finalAmount = (int) (amount * (1 + (boost / 100.0)));

        int currentXP = getXP(player.getUniqueId());
        int currentLevel = getLevel(player.getUniqueId());
        int newXP = currentXP + finalAmount;
        playerXP.put(player.getUniqueId(), newXP);

        player.sendMessage("§a+" + finalAmount + " XP" + (boost > 0 ? " (" + boost + "% Faction Boost)" : ""));

        int newLevel = getLevel(player.getUniqueId());
        if (newLevel > currentLevel) {
            player.sendTitle("§b§lLEVEL UP!", "§eYou are now level " + newLevel);
        }
    }

    public int getXP(UUID uuid) {
        return playerXP.getOrDefault(uuid, 0);
    }

    public int getLevel(UUID uuid) {
        int xp = getXP(uuid);
        Map.Entry<Integer, Integer> entry = levelUpXP.floorEntry(xp);
        return (entry != null) ? entry.getValue() : 1;
    }

    private boolean isLikelyLevelKeyed(Set<Integer> keys) {
        if (keys.isEmpty()) {
            return false;
        }
        List<Integer> sorted = new ArrayList<>(keys);
        Collections.sort(sorted);
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i) != i + 1) {
                return false;
            }
        }
        return true;
    }
}
