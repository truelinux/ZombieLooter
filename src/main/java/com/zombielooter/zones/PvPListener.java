package com.zombielooter.zones;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import com.zombielooter.ZombieLooterX;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvPListener implements Listener {
    private final ZombieLooterX plugin;
    private final ZoneManager zones;
    private final Map<UUID, Long> combatTagUntil = new HashMap<>();
    private int combatTagSeconds = 20;

    private final Map<UUID, String> lastRegion = new HashMap<>();

    public PvPListener(ZombieLooterX plugin, ZoneManager zones) {
        this.plugin = plugin;
        this.zones = zones;
        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "config.yml");
            cn.nukkit.utils.Config cfg = new cn.nukkit.utils.Config(f, cn.nukkit.utils.Config.YAML);
            combatTagSeconds = cfg.getInt("pvp.combat_tag_seconds", 20);
        } catch (Exception ignored) {}
    }

    private void tag(Player p) { combatTagUntil.put(p.getUniqueId(), System.currentTimeMillis() + combatTagSeconds * 1000L); }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) return;

        Player damager = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();

        boolean pvpAllowed = zones.isPvP(victim.getLocation()) && zones.isPvP(damager.getLocation());
        boolean safe = zones.isSafe(victim.getLocation()) || zones.isSafe(damager.getLocation());

        if (!pvpAllowed || safe) {
            e.setCancelled();
            damager.sendTip("§cPvP disabled in this zone.");
            return;
        }

        tag(damager); tag(victim);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (!zones.canBuild(event.getBlock().getLocation())) {
            event.setCancelled(); p.sendTip("§cYou cannot build here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!zones.canBuild(event.getBlock().getLocation())) {
            event.setCancelled(); p.sendTip("§cYou cannot break blocks here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (zones.isSafe(p.getLocation())) {
            Block b = event.getBlock();
            if (b != null && b.getId() == Block.ENDER_CHEST) {
                event.setCancelled();
                p.sendTip("§7Ender chests are disabled in this area.");
            }
        }
    }

    // Titles when entering or leaving regions
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        String currentName = null;
        Region r = zones.getRegionAt(p.getLocation());
        if (r != null) currentName = r.getName();

        String last = lastRegion.get(p.getUniqueId());
        if ((last == null && currentName != null) || (last != null && !last.equals(currentName))) {
            lastRegion.put(p.getUniqueId(), currentName);
            if (r == null) {
                p.sendTitle("§7Wilderness", "§8Build: allowed  •  PvP: depends", 10, 40, 10);
            } else {
                String pvp = r.getFlag("pvp", false) ? "§cON" : "§aOFF";
                String build = r.getFlag("build", true) ? "§aallowed" : "§cdenied";
                p.sendTitle("§6" + r.getName(), "§ePvP: " + pvp + " §7• §eBuild: " + build, 10, 40, 10);
            }
        }
    }
}
