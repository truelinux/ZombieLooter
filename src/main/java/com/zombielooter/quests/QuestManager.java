package com.zombielooter.quests;

import cn.nukkit.Player;
import cn.nukkit.entity.mob.EntityZombie;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import cn.nukkit.utils.TextFormat;

/**
 * QuestManager
 * - Loads quests from quests.yml (id, name, reward, objectives[])
 * - Tracks per-player QuestProgress (counters per objective)
 * - Persists progress to quest_progress.yml
 * - Listens for zombie kills to progress "kill" objectives
 * - Pays out rewards on completion via EconomyManager
 */
public class QuestManager implements Listener {

    private final ZombieLooterX plugin;
    private final Map<String, Quest> quests = new HashMap<>();
    private final Map<UUID, Map<String, QuestProgress>> progress = new HashMap<>();
    private Config progressConfig;
    private final Object progressLock = new Object();

    public QuestManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
        loadProgress();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Auto-save progress every 5 minutes
        plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(plugin,
                this::saveProgress, 20 * 60 * 5, 20 * 60 * 5);
    }

    /** Load quests from quests.yml */
    public final void load() {
        File f = new File(plugin.getDataFolder(), "quests.yml");
        if (!f.exists()) plugin.saveResource("quests.yml", false);

        Config cfg = new Config(f, Config.YAML);
        quests.clear();

        List<?> rawList = cfg.getList("quests");
        if (rawList == null || rawList.isEmpty()) {
            plugin.getLogger().warning("⚠ No quests found in quests.yml");
            return;
        }

        for (Object entry : rawList) {
            if (!(entry instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> qMap = (Map<String, Object>) entry;

            String id = String.valueOf(qMap.getOrDefault("id", UUID.randomUUID().toString()));
            String name = String.valueOf(qMap.getOrDefault("name", "Unnamed Quest"));
            Object rewardNode = qMap.get("reward");
            long reward = 50;
            if (rewardNode instanceof Number number) {
                reward = number.longValue();
            } else if (rewardNode != null) {
                plugin.getLogger().warning("Invalid reward for quest '" + id + "' (expected number). Defaulting to 50.");
            }

            Quest q = new Quest(id, name, reward);

            Object objList = qMap.get("objectives");
            if (objList instanceof List) {
                for (Object objEntry : (List<?>) objList) {
                    if (!(objEntry instanceof Map)) continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> oMap = (Map<String, Object>) objEntry;
                    String type = String.valueOf(oMap.getOrDefault("type", "none")).toLowerCase(Locale.ROOT);

                    if ("kill".equals(type)) {
                        String mob = String.valueOf(oMap.getOrDefault("mob", "zombie"));
                        Object rawCount = oMap.get("count");
                        int count = 1;
                        if (rawCount instanceof Number num) {
                            count = num.intValue();
                        } else if (rawCount != null) {
                            plugin.getLogger().warning("Invalid count for quest '" + id + "' objective (expected number). Defaulting to 1.");
                        }
                        q.addObjective(new KillObjective("kill_" + mob + "_" + count, mob, count));
                    } else {
                        plugin.getLogger().warning("⚠ Unknown objective type in quest " + id + ": " + type);
                    }
                }
            } else if (objList != null) {
                plugin.getLogger().warning("Objectives for quest '" + id + "' should be a list. Skipping invalid entry.");
            }
            quests.put(id.toLowerCase(Locale.ROOT), q);
        }

        plugin.getLogger().info("✅ Loaded quests: " + quests.size());
    }

    /**
     * Load quest progress from quest_progress.yml
     * Format:
     * players:
     *   <uuid>:
     *     <quest_id>:
     *       counters:
     *         <objective_id>: <count>
     */
    @SuppressWarnings("unchecked")
    public void loadProgress() {
        CompletableFuture.runAsync(() -> {
            try {
                File f = new File(plugin.getDataFolder(), "quest_progress.yml");
                if (!f.exists()) {
                    f.createNewFile();
                }
                Config localConfig = new Config(f, Config.YAML);

                Map<String, Object> playersData = localConfig.getSection("players").getAllMap();
                if (playersData == null) {
                    playersData = new HashMap<>();
                }

                int loadedPlayers = 0;
                int loadedQuests = 0;
                Map<UUID, Map<String, QuestProgress>> loadedProgress = new HashMap<>();

                for (Map.Entry<String, Object> playerEntry : playersData.entrySet()) {
                    try {
                        UUID playerId = UUID.fromString(playerEntry.getKey());

                        if (!(playerEntry.getValue() instanceof Map)) {
                            plugin.getLogger().warning("Quest progress for player " + playerEntry.getKey() + " is not a map; skipping.");
                            continue;
                        }
                        Map<String, Object> questsData = (Map<String, Object>) playerEntry.getValue();

                        Map<String, QuestProgress> playerProgress = new HashMap<>();

                        for (Map.Entry<String, Object> questEntry : questsData.entrySet()) {
                            String questId = questEntry.getKey();

                            if (!(questEntry.getValue() instanceof Map)) {
                                plugin.getLogger().warning("Quest progress entry for quest '" + questId + "' is not a map; skipping.");
                                continue;
                            }
                            Map<String, Object> questData = (Map<String, Object>) questEntry.getValue();

                            QuestProgress prog = new QuestProgress();

                            // Load counters
                            Object countersObj = questData.get("counters");
                            if (countersObj instanceof Map) {
                                Map<String, Object> counters = (Map<String, Object>) countersObj;
                                for (Map.Entry<String, Object> counter : counters.entrySet()) {
                                    if (counter.getValue() instanceof Number number) {
                                        prog.increment(counter.getKey(), number.intValue());
                                    } else if (counter.getValue() != null) {
                                        plugin.getLogger().warning("Invalid counter value for quest '" + questId + "' player " + playerEntry.getKey() + " (not a number)");
                                    }
                                }
                            }

                            playerProgress.put(questId.toLowerCase(Locale.ROOT), prog);
                            loadedQuests++;
                        }

                        loadedProgress.put(playerId, playerProgress);
                        loadedPlayers++;

                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in quest progress: " + playerEntry.getKey());
                    }
                }

                synchronized (progressLock) {
                    progress.clear();
                    progress.putAll(loadedProgress);
                    progressConfig = localConfig;
                }

                plugin.getLogger().info("✅ Loaded quest progress: " + loadedPlayers + " players, " + loadedQuests + " active quests");

            } catch (Exception e) {
                plugin.getLogger().error("Failed to load quest progress: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Save quest progress to quest_progress.yml asynchronously
     */
    public void saveProgress() {
        CompletableFuture.runAsync(() -> {
            try {
                Config cfg;
                Map<String, Object> playersData = new LinkedHashMap<>();

                synchronized (progressLock) {
                    cfg = this.progressConfig;
                    for (Map.Entry<UUID, Map<String, QuestProgress>> playerEntry : progress.entrySet()) {
                        Map<String, Object> questsData = new LinkedHashMap<>();

                        for (Map.Entry<String, QuestProgress> questEntry : playerEntry.getValue().entrySet()) {
                            Map<String, Object> questData = new LinkedHashMap<>();

                            Map<String, Integer> counters = new LinkedHashMap<>(questEntry.getValue().getAll());
                            if (!counters.isEmpty()) {
                                questData.put("counters", counters);
                            }

                            questsData.put(questEntry.getKey(), questData);
                        }

                        if (!questsData.isEmpty()) {
                            playersData.put(playerEntry.getKey().toString(), questsData);
                        }
                    }
                }

                if (cfg == null) {
                    plugin.getLogger().warning("Quest progress save skipped: progressConfig not initialized yet.");
                    return;
                }

                synchronized (progressLock) {
                    cfg.set("players", playersData);
                    cfg.save();
                }

                plugin.getLogger().debug("Quest progress saved for " + playersData.size() + " players");

            } catch (Exception e) {
                plugin.getLogger().error("Failed to save quest progress: " + e.getMessage(), e);
            }
        });
    }

    public void reload() {
        load();
        loadProgress();
        plugin.getLogger().info("Quests reloaded from quests.yml");
    }

    public Quest getQuest(String id) {
        if (id == null) return null;
        return quests.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Quest> getAll() {
        return quests.values();
    }

    public QuestProgress getProgress(Player p, String questId) {
        return getOrCreateProgress(p.getUniqueId(), questId.toLowerCase(Locale.ROOT));
    }

    /**
     * Get all active quests for a player
     */
    public Map<String, QuestProgress> getPlayerProgress(UUID playerId) {
        synchronized (progressLock) {
            Map<String, QuestProgress> perPlayer = progress.get(playerId);
            return perPlayer == null ? new HashMap<>() : new HashMap<>(perPlayer);
        }
    }

    private QuestProgress getOrCreateProgress(UUID playerId, String questId) {
        synchronized (progressLock) {
            Map<String, QuestProgress> perPlayer = progress.computeIfAbsent(playerId, k -> new HashMap<>());
            return perPlayer.computeIfAbsent(questId.toLowerCase(Locale.ROOT), k -> new QuestProgress());
        }
    }

    private Map<String, QuestProgress> getPlayerProgressSnapshot(UUID playerId) {
        synchronized (progressLock) {
            Map<String, QuestProgress> perPlayer = progress.get(playerId);
            return perPlayer == null ? Collections.emptyMap() : new HashMap<>(perPlayer);
        }
    }

    /** Try to complete quest; if done, pay reward and remove progress entry */
    public boolean completeIfReady(Player p, Quest q) {
        boolean completed;
        synchronized (progressLock) {
            QuestProgress prog = getOrCreateProgress(p.getUniqueId(), q.getId());
            if (!q.isComplete(prog)) {
                return false;
            }
            Map<String, QuestProgress> perPlayer = progress.get(p.getUniqueId());
            if (perPlayer != null) {
                perPlayer.remove(q.getId().toLowerCase(Locale.ROOT));
            }
            completed = true;
        }

        if (completed) {
            plugin.getEconomyManager().addBalance(p.getUniqueId(), (int) q.getRewardCoins());
            p.sendMessage(TextFormat.colorize('&', "&aQuest complete! Reward: &e" + q.getRewardCoins() + " coins"));
            p.sendTitle(TextFormat.colorize('&', "&6Quest Complete!"), TextFormat.colorize('&', "&e" + q.getName()), 10, 60, 10);

            saveProgress();
            return true;
        }
        return false;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EntityZombie)) return;
        if (e.getEntity().getLastDamageCause() == null) return;
        if (!(e.getEntity().getLastDamageCause().getEntity() instanceof Player)) return;

        Player killer = (Player) e.getEntity().getLastDamageCause().getEntity();
        boolean progressMade = false;

        synchronized (progressLock) {
            Map<String, QuestProgress> perPlayer = progress.get(killer.getUniqueId());
            if (perPlayer == null || perPlayer.isEmpty()) return;

            for (Map.Entry<String, QuestProgress> entry : perPlayer.entrySet()) {
                Quest q = quests.get(entry.getKey());
                if (q == null) continue;

                for (Objective o : q.getObjectives()) {
                    if (o instanceof KillObjective) {
                        o.onKill(killer, "zombie", entry.getValue());
                        progressMade = true;
                    }
                }
            }
        }

        if (progressMade) {
            for (Map.Entry<String, QuestProgress> entry : getPlayerProgressSnapshot(killer.getUniqueId()).entrySet()) {
                Quest q = quests.get(entry.getKey());
                if (q == null) continue;
                completeIfReady(killer, q);
            }
        }
    }

    // NEW: Save progress when player joins (in case of data)
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Progress will be loaded from file automatically
        UUID id = e.getPlayer().getUniqueId();
        Map<String, QuestProgress> active = getPlayerProgress(id);
        if (!active.isEmpty()) {
            e.getPlayer().sendMessage(TextFormat.colorize('&', "&7You have &e" + active.size() + " &7active quest(s)."));
        }
    }

    // NEW: Save progress when player quits
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Save immediately on quit to prevent data loss
        saveProgress();
    }
}
