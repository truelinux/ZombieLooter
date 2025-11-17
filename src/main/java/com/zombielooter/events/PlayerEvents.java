package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.network.protocol.ContainerOpenPacket;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.npc.VendorNPC;

public class PlayerEvents implements Listener {

    private final ZombieLooterX plugin;

    public PlayerEvents(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPacket(DataPacketSendEvent event) {
        if(event.getPacket() instanceof ContainerOpenPacket packet) {
            plugin.getLogger().info("Packet: " + packet.getWindowId() + " " + event.getPlayer().getWindowById(packet.windowId).getContents());
        }
    }


}
