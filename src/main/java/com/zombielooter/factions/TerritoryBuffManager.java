package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map;

public class TerritoryBuffManager {

    private final ZombieLooterX plugin;
    private final Config buffConfig;
    private final NavigableMap<Integer, BuffTier> buffTiers = new TreeMap<>();
    private final int incomeIntervalMinutes;

    private static class BuffTier {
        final double xpBoost;
        final double lootBoost;
        final int passiveIncome;

        BuffTier(double xp, double loot, int income) {
            this.xpBoost = xp;
            this.lootBoost = loot;
            this.passiveIncome = income;
        }
    }

    public TerritoryBuffManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "territory_buffs.yml");
        if (!configFile.exists()) {
            plugin.saveResource("territory_buffs.yml", false);
        }
        this.buffConfig = new Config(configFile, Config.YAML);
        loadBuffs();

        this.incomeIntervalMinutes = buffConfig.getInt("income-interval-minutes", 30);
        startIncomeTask();
    }

    private void loadBuffs() {
        buffTiers.clear();
        // ** THE FIX IS HERE **
        // Add a null check for the entire "tiers" section.
        ConfigSection tiersSection = buffConfig.getSection("tiers");
        if (tiersSection == null) {
            plugin.getLogger().warning("The 'tiers' section in territory_buffs.yml is missing. No territory buffs will be applied.");
            return;
        }

        for (String tierKey : tiersSection.getKeys(false)) {
            try {
                int requiredClaims = Integer.parseInt(tierKey);
                double xp = buffConfig.getDouble("tiers." + tierKey + ".xp-boost-percent", 0);
                double loot = buffConfig.getDouble("tiers." + tierKey + ".loot-boost-percent", 0);
                int income = buffConfig.getInt("tiers." + tierKey + ".passive-income-coins", 0);
                buffTiers.put(requiredClaims, new BuffTier(xp, loot, income));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid tier key in territory_buffs.yml: " + tierKey);
            }
        }
    }

    private BuffTier getBuffTierForFaction(Faction faction) {
        if (faction == null) return null;
        int claimCount = plugin.getClaimManager().getClaimCount(faction.getName());
        Map.Entry<Integer, BuffTier> entry = buffTiers.floorEntry(claimCount);
        return (entry != null) ? entry.getValue() : null;
    }

    public double getXpBoost(Player player) {
        Faction faction = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        BuffTier tier = getBuffTierForFaction(faction);
        return (tier != null) ? tier.xpBoost : 0;
    }

    public double getLootBoost(Player player) {
        Faction faction = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        BuffTier tier = getBuffTierForFaction(faction);
        return (tier != null) ? tier.lootBoost : 0;
    }

    private void startIncomeTask() {
        int intervalTicks = incomeIntervalMinutes * 60 * 20;
        if (intervalTicks <= 0) return;
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> {
            for (Faction faction : plugin.getFactionManager().getFactions()) {
                BuffTier tier = getBuffTierForFaction(faction);
                if (tier != null && tier.passiveIncome > 0) {
                    faction.depositToBank(tier.passiveIncome);
                }
            }
            plugin.getLogger().info("&aGranted passive income to all qualifying factions.");
        }, intervalTicks);
    }
}
