package com.zombielooter.economy;

import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final ZombieLooterX plugin;
    private final Config economyConfig;
    private final Map<UUID, Integer> balances = new HashMap<>();
    private final Map<String, Integer> factionBalances = new HashMap<>();

    public EconomyManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.economyConfig = new Config(new File(plugin.getDataFolder(), "economy.yml"), Config.YAML);
        loadBalances();
        loadFactionBalances();
    }

    private void loadBalances() {
        Map<String, Object> playerSection = economyConfig.getSection("players").getAll();
        playerSection.forEach((key, value) -> {
            try {
                UUID uuid = UUID.fromString(key);
                if (value instanceof Number number) {
                    balances.put(uuid, Math.max(0, number.intValue()));
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed UUID keys
            }
        });
    }

    private void loadFactionBalances() {
        Map<String, Object> factionSection = economyConfig.getSection("factions").getAll();
        factionSection.forEach((key, value) -> {
            if (value instanceof Number number) {
                factionBalances.put(key.toLowerCase(), Math.max(0, number.intValue()));
            }
        });
    }

    public int getBalance(UUID id){ return balances.getOrDefault(id, 0); }
    public void setBalance(UUID id, int amt){
        int safe = Math.max(0, amt);
        balances.put(id, safe);
        economyConfig.set("players." + id, safe);
        economyConfig.save();
    }
    public void addBalance(UUID id, int amt){ setBalance(id, getBalance(id)+Math.max(0, amt)); }
    public boolean withdraw(UUID id, int amt){
        int cur = getBalance(id);
        if (cur < amt) return false;
        setBalance(id, cur-amt);
        return true;
    }

    // Faction economy
    public int getFactionBalance(String faction){ return factionBalances.getOrDefault(faction.toLowerCase(), 0); }
    public void addFaction(String faction, int amt){
        String key = faction.toLowerCase();
        factionBalances.put(key, getFactionBalance(faction)+Math.max(0, amt));
        economyConfig.set("factions." + key, factionBalances.get(key));
        economyConfig.save();
    }
    public boolean withdrawFaction(String faction, int amt){
        int cur = getFactionBalance(faction);
        if (cur < amt) return false;
        String key = faction.toLowerCase();
        factionBalances.put(key, cur-amt);
        economyConfig.set("factions." + key, factionBalances.get(key));
        economyConfig.save();
        return true;
    }

    /**
     * Persist all in-memory balances to disk. This consolidates the latest
     * runtime state so that restarts never lose player or faction economy.
     */
    public void save() {
        Map<String, Object> players = new LinkedHashMap<>();
        balances.forEach((uuid, balance) -> players.put(uuid.toString(), balance));
        Map<String, Object> factions = new LinkedHashMap<>();
        factionBalances.forEach((faction, balance) -> factions.put(faction, balance));

        economyConfig.set("players", players);
        economyConfig.set("factions", factions);
        economyConfig.save();
    }
}
