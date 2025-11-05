package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import com.zombielooter.ZombieLooterX;

public class PlayerEvents  implements Listener {

    private ZombieLooterX plugin;

    public PlayerEvents(ZombieLooterX plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    }

}
