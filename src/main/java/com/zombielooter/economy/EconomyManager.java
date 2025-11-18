package com.zombielooter.economy;

import com.zombielooter.ZombieLooterX;

import cn.nukkit.utils.Config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final ZombieLooterX plugin;
    private final Config storage;
    private final Map<UUID, Integer> balances = new HashMap<>();
    private final Map<String, Integer> factionBalances = new HashMap<>();

    public EconomyManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.storage = new Config(new File(plugin.getDataFolder(), "economy.yml"), Config.YAML);
        load();
    }

    public int getBalance(UUID id){ return balances.getOrDefault(id, 0); }
    public void setBalance(UUID id, int amt){ balances.put(id, Math.max(0, amt)); save(); }
    public void addBalance(UUID id, int amt){ setBalance(id, getBalance(id)+Math.max(0, amt)); }
    public boolean withdraw(UUID id, int amt){
        int cur = getBalance(id);
        if (cur < amt) return false;
        setBalance(id, cur-amt);
        return true;
    }

    // Faction economy
    public int getFactionBalance(String faction){ return factionBalances.getOrDefault(faction.toLowerCase(), 0); }
    public void addFaction(String faction, int amt){ factionBalances.put(faction.toLowerCase(), getFactionBalance(faction)+Math.max(0, amt)); save(); }
    public boolean withdrawFaction(String faction, int amt){
        int cur = getFactionBalance(faction);
        if (cur < amt) return false;
        factionBalances.put(faction.toLowerCase(), cur-amt);
        save();
        return true;
    }

    public Map<UUID, Integer> getBalances() {
        return balances;
    }

    public Map<String, Integer> getFactionBalances() {
        return factionBalances;
    }

    private void load() {
        Map<String, Object> playerSection = storage.getSection("players");
        if (playerSection == null) {
            playerSection = new HashMap<>();
        }
        for (String key : playerSection.keySet()) {
            try {
                UUID id = UUID.fromString(key);
                balances.put(id, ((Number) playerSection.getOrDefault(key, 0)).intValue());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Map<String, Object> factionSection = storage.getSection("factions");
        if (factionSection == null) {
            factionSection = new HashMap<>();
        }
        for (String key : factionSection.keySet()) {
            int bal = ((Number) factionSection.getOrDefault(key, 0)).intValue();
            factionBalances.put(key.toLowerCase(), bal);
        }
    }

    public void save() {
        Map<String, Object> playerSection = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : balances.entrySet()) {
            playerSection.put(entry.getKey().toString(), entry.getValue());
        }
        storage.set("players", playerSection);

        Map<String, Object> factionSection = new HashMap<>();
        for (Map.Entry<String, Integer> entry : factionBalances.entrySet()) {
            factionSection.put(entry.getKey(), entry.getValue());
        }
        storage.set("factions", factionSection);
        storage.save();
    }
}
