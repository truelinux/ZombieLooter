package com.zombielooter.leaderboard;

import cn.nukkit.Player;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.EconomyManager;
import com.zombielooter.kitpvp.KitPvpManager;
import com.zombielooter.kitpvp.KitStats;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LeaderboardManager constructs and displays leaderboards, such as the
 * top balances (eco top).  It currently renders holograms near the player to
 * preview leaderboard standings without extra UI input.
 */
public class LeaderboardManager {
    private final ZombieLooterX plugin;
    private final Map<UUID, FloatingTextParticle> activeHolograms = new HashMap<>();

    public LeaderboardManager(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    public void showEconomyTop(Player player, int limit) {
        EconomyManager eco = plugin.getEconomyManager();
        List<Map.Entry<UUID, Integer>> top = eco.getBalances().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
        if (top.isEmpty()) {
            player.sendMessage(TextFormat.colorize('&', "&cNo balances recorded."));
            return;
        }
        StringBuilder text = new StringBuilder(TextFormat.colorize('&', "&6&lTop Balances"));
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            String name = resolveName(entry.getKey());
            text.append("\n").append(TextFormat.colorize('&', "&e" + (rank++) + ". &f" + name + " &7- &a$" + entry.getValue()));
        }
        sendHologram(player, text.toString());
    }

    public void showKitTop(Player player, int limit) {
        KitPvpManager kitPvp = plugin.getKitPvpManager();
        List<Map.Entry<UUID, KitStats>> top = kitPvp.getStats().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()))
                .limit(limit)
                .collect(Collectors.toList());
        if (top.isEmpty()) {
            player.sendMessage(TextFormat.colorize('&', "&cNo KitPvP stats recorded."));
            return;
        }
        StringBuilder text = new StringBuilder(TextFormat.colorize('&', "&6&lKitPvP Top Kills"));
        int rank = 1;
        for (Map.Entry<UUID, KitStats> entry : top) {
            String name = resolveName(entry.getKey());
            KitStats stats = entry.getValue();
            text.append("\n").append(TextFormat.colorize('&', "&e" + (rank++) + ". &f" + name + " &7- Kills: " + stats.getKills() + " K/D: " + String.format(Locale.US, "%.2f", stats.getKDR()))));
        }
        sendHologram(player, text.toString());
    }

    private String resolveName(UUID id) {
        String name = plugin.getServer().getOfflinePlayer(id).getName();
        if (name == null) {
            name = id.toString().substring(0, 8);
        }
        return name;
    }

    private void sendHologram(Player player, String text) {
        removeHologram(player.getUniqueId());
        Vector3 pos = player.add(0, 2, 0);
        FloatingTextParticle particle = new FloatingTextParticle(pos, text);
        player.getLevel().addParticle(particle, player);
        activeHolograms.put(player.getUniqueId(), particle);
        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> removeHologram(player.getUniqueId()), 20 * 10);
    }

    private void removeHologram(UUID id) {
        FloatingTextParticle old = activeHolograms.remove(id);
        if (old != null && old.getLevel() != null) {
            old.setInvisible(true);
            old.getLevel().addParticle(old);
        }
    }
}
