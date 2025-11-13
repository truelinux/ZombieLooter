package com.zombielooter.factions;

import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;

public class PowerManager {

    private final ZombieLooterX plugin;
    private final Config powerConfig;

    private final int defaultPower;
    private final int maxPower;
    private final int powerPerClaim;
    private final int onDeathLoss;
    private final int onKillGain;
    private final int onBossKillGain;
    private final int raidablePowerBuffer;
    private final int raidWindowMinutes;

    public PowerManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "power.yml");
        if (!configFile.exists()) {
            plugin.saveResource("power.yml", false);
        }
        this.powerConfig = new Config(configFile, Config.YAML);

        this.defaultPower = powerConfig.getInt("default-power", 10);
        this.maxPower = powerConfig.getInt("max-power", 100);
        this.powerPerClaim = powerConfig.getInt("power-per-claim", 5);
        this.onDeathLoss = powerConfig.getInt("player-interactions.on-death", -3);
        this.onKillGain = powerConfig.getInt("player-interactions.on-kill", 2);
        this.onBossKillGain = powerConfig.getInt("pve-events.on-boss-kill", 15);
        this.raidablePowerBuffer = powerConfig.getInt("raiding.raidable-power-buffer", -10);
        this.raidWindowMinutes = powerConfig.getInt("raiding.raid-window-minutes", 60);
    }

    public int getDefaultPower() {
        return defaultPower;
    }

    public int getMaxPower() {
        return maxPower;
    }

    public int getPowerPerClaim() {
        return powerPerClaim;
    }

    public int getOnDeathLoss() {
        return onDeathLoss;
    }

    public int getOnKillGain() {
        return onKillGain;
    }

    public int getOnBossKillGain() {
        return onBossKillGain;
    }

    public int getRaidablePowerBuffer() {
        return raidablePowerBuffer;
    }

    public int getRaidWindowMinutes() {
        return raidWindowMinutes;
    }

    public void addPower(Faction faction, int amount) {
        if (faction == null) return;
        int currentPower = faction.getPower();
        int newPower = Math.min(maxPower, currentPower + amount);
        faction.setPower(newPower);
    }

    public void removePower(Faction faction, int amount) {
        if (faction == null) return;
        int currentPower = faction.getPower();
        // Power cannot drop below zero
        int newPower = Math.max(0, currentPower - amount);
        faction.setPower(newPower);
    }
}
