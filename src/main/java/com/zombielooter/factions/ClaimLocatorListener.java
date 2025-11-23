package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight, single-use locator (Ender Pearl styled) that points toward the nearest
 * faction claim in the current wild world. Intended to be useful but not overpowered.
 */
public class ClaimLocatorListener implements Listener {
    public static final String LOCATOR_NAME = TextFormat.colorize('&', "&dClaim Seeker");
    private static final long COOLDOWN_MS = 30_000;
    private static final int MAX_RANGE_BLOCKS = 1200;

    private final ClaimManager claimManager;
    private final ZombieLooterX plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ClaimLocatorListener(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.claimManager = plugin.getClaimManager();
    }

    public static Item createLocatorItem(int amount) {
        Item pearl = Item.get(Item.ENDER_PEARL, 0, amount);
        pearl.setCustomName(LOCATOR_NAME);
        pearl.setLore(
                TextFormat.colorize('&', "&7Tracks the nearest faction claim"),
                TextFormat.colorize('&', "&7in this wild world. Single-use."),
                TextFormat.colorize('&', "&8Cooldown: 30s  Range: 1200 blocks")
        );
        return pearl;
    }

    public static boolean isLocator(Item item) {
        return item != null && item.getId() == Item.ENDER_PEARL && LOCATOR_NAME.equals(item.getCustomName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        Item item = event.getItem();
        if (!isLocator(item)) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!plugin.getWorldPortalManager().isWildWorld(player.getLevel())) {
            player.sendMessage(TextFormat.colorize('&', "&cThe Claim Seeker only hums in wild worlds."));
            return;
        }

        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            long seconds = (readyAt - now) / 1000;
            player.sendPopup(TextFormat.colorize('&', "&cCooling down: &e" + seconds + "s"));
            return;
        }

        ClaimManager.ClaimLocation nearest = claimManager.findNearestClaim(
                player.getLevel(),
                player.getChunkX(),
                player.getChunkZ());
        if (nearest == null) {
            player.sendMessage(TextFormat.colorize('&', "&7The seeker stays silent. No claims detected in this wild world."));
            return;
        }

        double dx = nearest.centerBlockX() - player.getX();
        double dz = nearest.centerBlockZ() - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > MAX_RANGE_BLOCKS) {
            player.sendMessage(TextFormat.colorize('&', "&7The pulse is too faint. Move deeper into the wilds."));
            return;
        }

        Vector3 target = new Vector3(nearest.centerBlockX(), player.getLevel().getHighestBlockAt(nearest.centerBlockX(), nearest.centerBlockZ()) + 1, nearest.centerBlockZ());
        for (int i = 0; i < 12; i++) {
            double t = (i + 1) / 12.0;
            double px = player.getX() + dx * t;
            double pz = player.getZ() + dz * t;
            player.getLevel().addParticleEffect(new Vector3(px, player.getY() + 1.0, pz), ParticleEffect.REDSTONE_ORE_DUST);
        }

        player.sendMessage(TextFormat.colorize('&',
                "&dSeeker locked onto faction &b" + nearest.faction +
                        " &7(~" + (int) distance + " blocks). Follow the spark trail!"));

        if (!player.isCreative()) {
            item.setCount(item.getCount() - 1);
            player.getInventory().setItemInHand(item);
        }
        cooldowns.put(player.getUniqueId(), now + COOLDOWN_MS);
    }
}
