package com.zombielooter.leaderboard;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.EconomyManager;
import com.zombielooter.kitpvp.KitPvpManager;
import com.zombielooter.kitpvp.KitStats;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * LeaderboardManager constructs and displays leaderboards, such as the
 * top balances (eco top).  It renders short-lived holograms near a player
 * on demand and also supports persistent holograms configured in
 * {@code leaderboards.yml} that auto-refresh.
 */
public class LeaderboardManager {
    private final ZombieLooterX plugin;
    private final Config config;
    private final Map<UUID, FloatingTextParticle> activeHolograms = new HashMap<>();
    private final Map<String, BoardSlot> persistentBoards = new HashMap<>();
    private TaskHandler refreshTask;
    private int refreshSeconds = 30;

    private enum BoardType { ECONOMY, KITPVP }

    private static class BoardSlot {
        private final BoardType type;
        private final int limit;
        private final String title;
        private final FloatingTextParticle particle;

        private BoardSlot(BoardType type, int limit, String title, FloatingTextParticle particle) {
            this.type = type;
            this.limit = limit;
            this.title = title;
            this.particle = particle;
        }
    }

    public LeaderboardManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        plugin.saveResource("leaderboards.yml", false);
        this.config = new Config(new File(plugin.getDataFolder(), "leaderboards.yml"), Config.YAML);
        this.refreshSeconds = config.getInt("leaderboards.refresh-seconds", 30);
        loadPersistentBoards();
        startRefreshTask();
    }

    public void showEconomyTop(Player player, int limit) {
        String text = buildEconomyText(limit, TextFormat.colorize('&', "&6&lTop Balances"));
        if (text == null) {
            player.sendMessage(TextFormat.colorize('&', "&cNo balances recorded."));
            return;
        }
        sendHologram(player, text);
    }

    public void showKitTop(Player player, int limit) {
        String text = buildKitText(limit, TextFormat.colorize('&', "&6&lKitPvP Top Kills"));
        if (text == null) {
            player.sendMessage(TextFormat.colorize('&', "&cNo KitPvP stats recorded."));
            return;
        }
        sendHologram(player, text);
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (UUID id : new java.util.HashSet<>(activeHolograms.keySet())) {
            removeHologram(id);
        }
        persistentBoards.values().forEach(slot -> {
            FloatingTextParticle p = slot.particle;
            if (p != null && p.getLevel() != null) {
                p.setInvisible(true);
                p.getLevel().addParticle(p);
            }
        });
        persistentBoards.clear();
    }

    private void loadPersistentBoards() {
        persistentBoards.values().forEach(slot -> despawn(slot.particle));
        persistentBoards.clear();

        List<Map<String, Object>> economyBoards = config.getMapList("leaderboards.economy");
        List<Map<String, Object>> kitBoards = config.getMapList("leaderboards.kitpvp");

        int index = 0;
        for (Map<String, Object> data : economyBoards) {
            BoardSlot slot = createSlot(BoardType.ECONOMY, data);
            if (slot != null) {
                persistentBoards.put("eco-" + (index++), slot);
            }
        }

        index = 0;
        for (Map<String, Object> data : kitBoards) {
            BoardSlot slot = createSlot(BoardType.KITPVP, data);
            if (slot != null) {
                persistentBoards.put("kit-" + (index++), slot);
            }
        }

        refreshPersistentBoards();
    }

    private BoardSlot createSlot(BoardType type, Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Level defaultLevel = plugin.getServer().getDefaultLevel();
        String world = (String) data.getOrDefault("world", defaultLevel != null ? defaultLevel.getName() : "");
        if (world == null || world.isEmpty()) {
            if (!plugin.getServer().getLevels().isEmpty()) {
                world = plugin.getServer().getLevels().values().iterator().next().getName();
            }
        }
        if (world == null || world.isEmpty()) {
            return null;
        }
        if (!plugin.getServer().isLevelLoaded(world)) {
            plugin.getServer().loadLevel(world);
        }
        Level level = plugin.getServer().getLevelByName(world);
        if (level == null) {
            return null;
        }

        double x = ((Number) data.getOrDefault("x", 0)).doubleValue();
        double y = ((Number) data.getOrDefault("y", 70)).doubleValue();
        double z = ((Number) data.getOrDefault("z", 0)).doubleValue();
        int limit = ((Number) data.getOrDefault("limit", 5)).intValue();
        String title = TextFormat.colorize('&', (String) data.getOrDefault("title", type == BoardType.ECONOMY ? "&6Top Balances" : "&6KitPvP Top"));

        FloatingTextParticle particle = new FloatingTextParticle(new Vector3(x, y, z), "");
        level.addParticle(particle);
        return new BoardSlot(type, limit, title, particle);
    }

    private void startRefreshTask() {
        if (refreshSeconds <= 0) {
            refreshSeconds = 30;
        }
        refreshTask = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, this::refreshPersistentBoards, refreshSeconds * 20L);
    }

    private void refreshPersistentBoards() {
        for (BoardSlot slot : persistentBoards.values()) {
            String text = slot.type == BoardType.ECONOMY
                    ? buildEconomyText(slot.limit, slot.title)
                    : buildKitText(slot.limit, slot.title);
            if (text == null) {
                continue;
            }
            slot.particle.setText(text);
            if (slot.particle.getLevel() != null) {
                slot.particle.getLevel().addParticle(slot.particle);
            }
        }
    }

    private String buildEconomyText(int limit, String title) {
        EconomyManager eco = plugin.getEconomyManager();
        if (eco == null) {
            return null;
        }
        List<Map.Entry<UUID, Integer>> top = eco.getTopBalances(limit);
        if (top.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder(title);
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            String name = resolveName(entry.getKey());
            text.append("\n").append(TextFormat.colorize('&', "&e" + (rank++) + ". &f" + name + " &7- &a$" + entry.getValue()));
        }
        return text.toString();
    }

    private String buildKitText(int limit, String title) {
        KitPvpManager kitPvp = plugin.getKitPvpManager();
        if (kitPvp == null) {
            return null;
        }
        List<Map.Entry<UUID, KitStats>> top = kitPvp.getTopKillers(limit);
        if (top.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder(title);
        int rank = 1;
        for (Map.Entry<UUID, KitStats> entry : top) {
            String name = resolveName(entry.getKey());
            KitStats stats = entry.getValue();
            text.append("\n").append(TextFormat.colorize('&', "&e" + (rank++) + ". &f" + name + " &7- Kills: " + stats.getKills() + " K/D: " + String.format(Locale.US, "%.2f", stats.getKDR()))));
        }
        return text.toString();
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

    private void despawn(FloatingTextParticle particle) {
        if (particle != null && particle.getLevel() != null) {
            particle.setInvisible(true);
            particle.getLevel().addParticle(particle);
        }
    }
}
