package com.zombielooter.security;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import com.zombielooter.ZombieLooterX;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiCheatBasic implements Listener {
    private final ZombieLooterX plugin;
    private final Map<UUID, Long> lastHit = new HashMap<>();

    public AntiCheatBasic(ZombieLooterX plugin){ this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) e;
        if (!(ev.getDamager() instanceof Player)) return;
        Player p = (Player) ev.getDamager();
        long now = System.currentTimeMillis();
        long prev = lastHit.getOrDefault(p.getUniqueId(), 0L);
        long delta = now - prev;
        if (delta < 60) { // < 60ms between hits is suspicious for autoclick
            // soft mitigation: cancel occasionally
            if (delta < 45) {
                e.setCancelled();
                p.sendPopup("§cSwing too fast!");
            }
        }
        lastHit.put(p.getUniqueId(), now);
    }
}
