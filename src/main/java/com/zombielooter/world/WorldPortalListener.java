package com.zombielooter.world;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerMoveEvent;

public class WorldPortalListener implements Listener {
    private final WorldPortalManager manager;

    public WorldPortalListener(WorldPortalManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        manager.handleMove(event);
    }
}
