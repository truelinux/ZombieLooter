package com.zombielooter.economy;

import com.zombielooter.ZombieLooterX;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final ZombieLooterX plugin;
    private final Map<UUID, Integer> balances = new HashMap<>();
    private final Map<String, Integer> factionBalances = new HashMap<>();

    public EconomyManager(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    public int getBalance(UUID id){ return balances.getOrDefault(id, 0); }
    public void setBalance(UUID id, int amt){ balances.put(id, Math.max(0, amt)); }
    public void addBalance(UUID id, int amt){ setBalance(id, getBalance(id)+Math.max(0, amt)); }
    public boolean withdraw(UUID id, int amt){
        int cur = getBalance(id);
        if (cur < amt) return false;
        setBalance(id, cur-amt);
        return true;
    }

    // Faction economy
    public int getFactionBalance(String faction){ return factionBalances.getOrDefault(faction.toLowerCase(), 0); }
    public void addFaction(String faction, int amt){ factionBalances.put(faction.toLowerCase(), getFactionBalance(faction)+Math.max(0, amt)); }
    public boolean withdrawFaction(String faction, int amt){
        int cur = getFactionBalance(faction);
        if (cur < amt) return false;
        factionBalances.put(faction.toLowerCase(), cur-amt);
        return true;
    }
}
