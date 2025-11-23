package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import cn.nukkit.utils.TextFormat;

public class RaidManager {

    private final ZombieLooterX plugin;
    private final GUITextManager text;
    private final Config raidConfig;
    private final Map<String, Long> raidableFactions = new ConcurrentHashMap<>();
    private ActiveRaid activeRaid = null;

    private final int bannerDefenseDuration;
    private final String bannerItemId;
    private final String bannerItemName;
    private final String[] bannerItemLore;
    private final int bannerCost;

    static class ActiveRaid {
        final Faction attacker;
        final Faction defender;
        final Location bannerLocation;
        final long raidEndTime;

        ActiveRaid(Faction attacker, Faction defender, Location bannerLocation, int durationSeconds) {
            this.attacker = attacker;
            this.defender = defender;
            this.bannerLocation = bannerLocation;
            this.raidEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        }
    }

    public RaidManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.text = plugin.getGUITextManager();
        File configFile = new File(plugin.getDataFolder(), "raid.yml");
        if (!configFile.exists()) {
            plugin.saveResource("raid.yml", false);
        }
        this.raidConfig = new Config(configFile, Config.YAML);

        this.bannerDefenseDuration = raidConfig.getInt("banner-defense-duration", 300);
        this.bannerItemId = raidConfig.getString("banner-item-id", "minecraft:banner");
        this.bannerItemName = raidConfig.getString("banner-item-name", "&c&lRaid Banner");
        this.bannerItemLore = raidConfig.getStringList("banner-item-lore").toArray(new String[0]);
        this.bannerCost = raidConfig.getInt("banner-cost", 5000);

        startRaidTimerTask();
    }

    public Item getRaidBanner() {
        Item banner = Item.get(bannerItemId, 0, 1);
        banner.setCustomName(bannerItemName);
        banner.setLore(bannerItemLore);

        if (banner.getNamedTag() == null) {
            banner.setNamedTag(new CompoundTag());
        }
        banner.getNamedTag().putBoolean("zlxRaidBanner", true);
        return banner;
    }

    public int getBannerCost() {
        return bannerCost;
    }

    public boolean isRaidBanner(Item item) {
        return item.getNamedTag() != null && item.getNamedTag().contains("zlxRaidBanner");
    }

    public void startRaid(Player player, Faction attacker, Faction defender, Location bannerLocation) {
        if (activeRaid != null) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.raid.already_active", "&cThere is already an active raid in progress on the server!")));
            return;
        }
        if (!isFactionRaidable(defender.getName())) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.raid.not_raidable", "&cThis faction is no longer raidable.")));
            return;
        }

        activeRaid = new ActiveRaid(attacker, defender, bannerLocation, bannerDefenseDuration);
        plugin.getServer().broadcastMessage(String.format(text.getText("commands.raid.started_broadcast", "&c&lRAID STARTED: &e%s is attacking %s!"), attacker.getName(), defender.getName()));
        plugin.getServer().broadcastMessage(String.format(text.getText("commands.raid.started_info", "&7Defend the banner for %d minutes to capture the territory!"), bannerDefenseDuration / 60));
    }

    public void endRaid(boolean attackerWon) {
        if (activeRaid == null) return;

        if (attackerWon) {
            plugin.getClaimManager().forceClaimChunk(
                    activeRaid.attacker,
                    activeRaid.bannerLocation.getLevel(),
                    activeRaid.bannerLocation.getChunkX(),
                    activeRaid.bannerLocation.getChunkZ());
            plugin.getServer().broadcastMessage(String.format(text.getText("commands.raid.ended_success_broadcast", "&c&lRAID ENDED: &e%s has successfully captured the territory from %s!"), activeRaid.attacker.getName(), activeRaid.defender.getName()));
        } else {
            plugin.getServer().broadcastMessage(String.format(text.getText("commands.raid.ended_fail_broadcast", "&a&lRAID FAILED: &e%s has successfully defended their territory!"), activeRaid.defender.getName()));
        }
        
        activeRaid.bannerLocation.getLevel().setBlock(activeRaid.bannerLocation, Block.get(Block.AIR));
        activeRaid = null;
    }

    private void startRaidTimerTask() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> {
            if (activeRaid == null) return;

            long remainingSeconds = (activeRaid.raidEndTime - System.currentTimeMillis()) / 1000;

            if (remainingSeconds <= 0) {
                endRaid(true); // Attacker wins
                return;
            }

            String message = String.format(text.getText("commands.raid.timer_title", "&c&lRAID IN PROGRESS: &e%ds"), remainingSeconds);
            for (UUID uuid : activeRaid.attacker.getMembers()) {
                plugin.getServer().getPlayer(uuid).ifPresent(p -> p.sendTitle(message));
            }
            for (UUID uuid : activeRaid.defender.getMembers()) {
                plugin.getServer().getPlayer(uuid).ifPresent(p -> p.sendTitle(message));
            }

        }, 20);
    }

    public ActiveRaid getActiveRaid() {
        return activeRaid;
    }

    public void updateFactionRaidableStatus(Faction faction) {
        if (faction == null) return;
        int landValue = plugin.getClaimManager().getClaimCount(faction.getName()) * plugin.getPowerManager().getPowerPerClaim();
        int raidableThreshold = landValue + plugin.getPowerManager().getRaidablePowerBuffer();
        if (faction.getPower() < raidableThreshold) {
            long raidEndTime = System.currentTimeMillis() + (plugin.getPowerManager().getRaidWindowMinutes() * 60 * 1000);
            if (!isFactionRaidable(faction.getName())) {
                plugin.getServer().broadcastMessage(String.format(text.getText("commands.raid.raidable_alert", "&c&lRAID ALERT: &e%s is now raidable for the next %d minutes!"), faction.getName(), plugin.getPowerManager().getRaidWindowMinutes()));
            }
            raidableFactions.put(faction.getName(), raidEndTime);
        } else {
            if (raidableFactions.remove(faction.getName()) != null) {
                plugin.getServer().broadcastMessage(String.format(text.getText("commands.raid.secured_alert", "&a&lSECURED: &e%s is no longer raidable."), faction.getName()));
            }
        }
    }

    public boolean isFactionRaidable(String factionName) {
        if (!raidableFactions.containsKey(factionName)) return false;
        if (System.currentTimeMillis() > raidableFactions.get(factionName)) {
            raidableFactions.remove(factionName);
            return false;
        }
        return true;
    }
}
