package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;
import cn.nukkit.utils.TextFormat;

public class RaidListener implements Listener {

    private final ZombieLooterX plugin;
    private final GUITextManager text;
    private final RaidManager raidManager;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;

    public RaidListener(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.text = plugin.getGUITextManager();
        this.raidManager = plugin.getRaidManager();
        this.factionManager = plugin.getFactionManager();
        this.claimManager = plugin.getClaimManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!raidManager.isRaidBanner(event.getItem())) {
            return;
        }

        // Prevent placing the banner in your own or unclaimed land
        Faction defendingFaction = claimManager.getFactionForChunk(event.getBlock().getLevel(), event.getBlock().getChunkX(), event.getBlock().getChunkZ());
        if (defendingFaction == null) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.raid_listener.place_in_claimed_only", "&cYou can only place a raid banner in claimed territory.")));
            event.setCancelled(true);
            return;
        }

        Faction attackingFaction = factionManager.getFactionByPlayer(player.getUniqueId());
        if (attackingFaction == null || attackingFaction.equals(defendingFaction)) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.raid_listener.cannot_raid_self", "&cYou cannot raid your own faction.")));
            event.setCancelled(true);
            return;
        }

        // Start the raid
        raidManager.startRaid(player, attackingFaction, defendingFaction, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        RaidManager.ActiveRaid activeRaid = raidManager.getActiveRaid();
        if (activeRaid == null) {
            return;
        }

        // Check if the broken block is the raid banner
        if (event.getBlock().getLocation().equals(activeRaid.bannerLocation)) {
            Player player = event.getPlayer();
            Faction playerFaction = factionManager.getFactionByPlayer(player.getUniqueId());

            // Only members of the defending faction can destroy the banner
            if (playerFaction == null || !playerFaction.equals(activeRaid.defender)) {
                player.sendMessage(TextFormat.colorize('&', text.getText("commands.raid_listener.cannot_destroy_banner", "&cYou cannot destroy the enemy's raid banner!")));
                event.setCancelled(true);
                return;
            }

            // Defender destroyed the banner, raid fails
            raidManager.endRaid(false);
            event.setCancelled(true); // Prevent the banner from dropping as an item
        }
    }
}
