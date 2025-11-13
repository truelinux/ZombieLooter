package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.npc.VendorNPC;

public class PlayerEvents implements Listener {

    private final ZombieLooterX plugin;

    public PlayerEvents(ZombieLooterX plugin) {
        this.plugin = plugin;
    }


}
