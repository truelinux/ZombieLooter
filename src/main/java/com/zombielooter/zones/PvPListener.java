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
import com.zombielooter.factions.Faction;
import com.zombielooter.gui.FeedbackUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import cn.nukkit.utils.TextFormat;

public class PvPListener implements Listener {
    private final ZombieLooterX plugin;
    private final ZoneManager zones;
    private final Map<UUID, Long> combatTagUntil = new HashMap<>();
    private int combatTagSeconds = 20;

    private final Map<UUID, String> lastRegion = new HashMap<>();
    private final Map<UUID, String> lastFactionClaim = new HashMap<>(); // To track entering/leaving faction land

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

        boolean safeVictim = zones.isSafe(victim.getLocation());
        boolean safeDamager = zones.isSafe(damager.getLocation());
        if (safeVictim || safeDamager) {
            e.setCancelled();
            FeedbackUtil.toastError(damager,"&cCombat disabled in safe zone.");
            return;
        }

        boolean pvpAllowed = zones.isPvP(victim.getLocation()) && zones.isPvP(damager.getLocation());

        if (!pvpAllowed) {
            e.setCancelled();
            FeedbackUtil.toastError(damager,"&cPvP disabled in this zone.");
            return;
        }

        tag(damager); tag(victim);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        boolean safeZone = zones.isSafe(player.getLocation());
        if (!safeZone) return;

        // Allow mob damage during infection outbreak in spawn world, but still block PvP.
        boolean outbreakActive = plugin.getInfectionManager() != null && plugin.getInfectionManager().isOutbreakActive()
                && plugin.getInfectionManager().isSpawnWorld(player.getLevel());
        if (outbreakActive && !(event instanceof EntityDamageByEntityEvent dmg && dmg.getDamager() instanceof Player)) {
            return;
        }

        event.setCancelled();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (!zones.canBuild(event.getBlock().getLocation()) && !p.isOp()) {
            event.setCancelled(); FeedbackUtil.toastError(p, "&cYou cannot build here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!zones.canBuild(event.getBlock().getLocation()) && !p.isOp()) {
            event.setCancelled(); FeedbackUtil.toastError(p, "&cYou cannot break blocks here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (zones.isSafe(p.getLocation())) {
            Block b = event.getBlock();
            if (b != null && b.getId() == Block.ENDER_CHEST && !p.isOp()) {
                event.setCancelled();
                FeedbackUtil.toastError(p, "&7Ender chests are disabled in this area.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        // Optimization: only run checks if the player has moved to a new chunk
        if (event.getFrom().getChunkX() == event.getTo().getChunkX() && event.getFrom().getChunkZ() == event.getTo().getChunkZ()) {
            return;
        }

        // --- Region Title Logic ---
        String currentRegionName = null;
        Region r = zones.getRegionAt(p.getLocation());
        if (r != null) currentRegionName = r.getName();

        String last = lastRegion.get(p.getUniqueId());
        if ((last == null && currentRegionName != null) || (last != null && !last.equals(currentRegionName))) {
            lastRegion.put(p.getUniqueId(), currentRegionName);
            if (r == null) {
                p.sendTitle(TextFormat.colorize('&', "&7Wilderness"), TextFormat.colorize('&', "&8Build: allowed  •  PvP: depends  •  Safe: off"), 10, 40, 10);
            } else {
                String pvp = r.getFlag("pvp", false) ? "&cON" : "&aOFF";
                String build = r.getFlag("build", true) ? "&aON" : "&cOFF";
                String safe = r.getFlag("safe", false) ? "&aON" : "&cOFF";
                p.sendTitle(TextFormat.colorize('&', "&6" + r.getName()), TextFormat.colorize('&', "&ePvP: " + pvp + " &7• &eBuild: " + build + " &7• &eSafe: " + safe), 10, 40, 10);
            }
        }

        // --- Faction Raid Warning Logic ---
        Faction currentFaction = plugin.getClaimManager().getFactionForChunk(p.getLevel(), p.getChunkX(), p.getChunkZ());
        String currentFactionName = (currentFaction != null) ? currentFaction.getName() : "Wilderness";
        String lastFaction = lastFactionClaim.getOrDefault(p.getUniqueId(), "Wilderness");

        if (!currentFactionName.equals(lastFaction)) {
            lastFactionClaim.put(p.getUniqueId(), currentFactionName);
            if (currentFaction != null && plugin.getRaidManager().isFactionRaidable(currentFaction.getName())) {
                p.sendTitle(TextFormat.colorize('&', "&c&lDANGER"), TextFormat.colorize('&', "&eEntering raidable territory of " + currentFaction.getName()), 10, 60, 20);
            }
        }
    }
}
