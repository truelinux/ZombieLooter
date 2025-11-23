package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerFoodLevelChangeEvent;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.network.protocol.ContainerOpenPacket;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.DummyBossBar;

import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.HUDManager;
import com.zombielooter.npc.VendorNPC;
import me.skh6075.pnx.graphicscore.GraphicScore;
import me.skh6075.pnx.graphicscore.player.ScorePlayerManager;

public class PlayerEvents implements Listener {

    private final ZombieLooterX plugin;

    public PlayerEvents(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        var bossBar = new cn.nukkit.utils.DummyBossBar.Builder(player)
                        .color(cn.nukkit.utils.BossBarColor.PURPLE)
                        .build();
        player.createBossBar(bossBar);
        plugin.getServer().getScheduler().scheduleDelayedTask(new Task() {
            @Override
            public void onRun(int arg0) {
                plugin.getWorldPortalManager().handleJoin(player);

            }
        }, 20);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getWorldPortalManager().handleQuit(event.getPlayer());
        // Destroy any boss bars when the player leaves to avoid leftover handles
        event.getPlayer().getDummyBossBars().values().forEach(DummyBossBar::destroy);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(PlayerFoodLevelChangeEvent event) {
        Player player = event.getPlayer();
        String spawnWorld = plugin.getWorldPortalManager().getSpawnWorld();
        if (player.getLevel() != null && player.getLevel().getName().equalsIgnoreCase(spawnWorld)) {
            event.setCancelled(true);
        }
    }


}
