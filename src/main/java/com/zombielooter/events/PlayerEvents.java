package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.network.protocol.ContainerOpenPacket;
import cn.nukkit.utils.BossBarColor;
import cn.nukkit.utils.DummyBossBar;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.npc.VendorNPC;

public class PlayerEvents implements Listener {

    private final ZombieLooterX plugin;
    private final boolean logContainerPackets;

    public PlayerEvents(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.logContainerPackets = plugin.getConfig().getBoolean("debug.log-container-open-packets", false);
    }

    @EventHandler
    public void onPacket(DataPacketSendEvent event) {
        if (!logContainerPackets) {
            return;
        }
        if(event.getPacket() instanceof ContainerOpenPacket packet) {
            var inventory = event.getPlayer().getWindowById(packet.windowId);
            if (inventory == null) {
                plugin.getLogger().debug("ContainerOpenPacket windowId " + packet.windowId + " had no matching inventory for " + event.getPlayer().getName());
                return;
            }
            plugin.getLogger().info("Packet: " + packet.getWindowId() + " " + inventory.getContents());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DummyBossBar bossBar = new DummyBossBar.Builder(player).color(BossBarColor.PURPLE).build();
        player.createBossBar(bossBar);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Destroy any boss bars when the player leaves to avoid leftover handles
        event.getPlayer().getDummyBossBars().values().forEach(DummyBossBar::destroy);
    }


}
