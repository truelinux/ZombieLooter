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

    public QuestManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
        loadProgress();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Auto-save progress every 5 minutes
        plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(plugin, 
            () -> saveProgress(), 20 * 60 * 5, 20 * 60 * 5);
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
            long reward = ((Number) qMap.getOrDefault("reward", 50)).longValue();

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
                        int count = ((Number) oMap.getOrDefault("count", 1)).intValue();
                        q.addObjective(new KillObjective("kill_" + mob + "_" + count, mob, count));
                    } else {
                        plugin.getLogger().warning("⚠ Unknown objective type in quest " + id + ": " + type);
                    }
                }
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
                progressConfig = new Config(f, Config.YAML);
                
                Map<String, Object> playersData = progressConfig.getSection("players").getAllMap();
                if (playersData == null || playersData.isEmpty()) {
                    plugin.getLogger().info("No quest progress data found (fresh start).");
                    return;
                }

                int loadedPlayers = 0;
                int loadedQuests = 0;

                for (Map.Entry<String, Object> playerEntry : playersData.entrySet()) {
                    try {
                        UUID playerId = UUID.fromString(playerEntry.getKey());
                        
                        if (!(playerEntry.getValue() instanceof Map)) continue;
                        Map<String, Object> questsData = (Map<String, Object>) playerEntry.getValue();

                        Map<String, QuestProgress> playerProgress = new HashMap<>();

                        for (Map.Entry<String, Object> questEntry : questsData.entrySet()) {
                            String questId = questEntry.getKey();
                            
                            if (!(questEntry.getValue() instanceof Map)) continue;
                            Map<String, Object> questData = (Map<String, Object>) questEntry.getValue();

                            QuestProgress prog = new QuestProgress();

                            // Load counters
                            Object countersObj = questData.get("counters");
                            if (countersObj instanceof Map) {
                                Map<String, Object> counters = (Map<String, Object>) countersObj;
                                for (Map.Entry<String, Object> counter : counters.entrySet()) {
                                    if (counter.getValue() instanceof Number) {
                                        prog.increment(counter.getKey(), 
                                            ((Number) counter.getValue()).intValue());
                                    }
                                }
                            }

                            playerProgress.put(questId.toLowerCase(Locale.ROOT), prog);
                            loadedQuests++;
                        }

                        progress.put(playerId, playerProgress);
                        loadedPlayers++;

                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in quest progress: " + playerEntry.getKey());
                    }
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
                Map<String, Object> playersData = new LinkedHashMap<>();

                for (Map.Entry<UUID, Map<String, QuestProgress>> playerEntry : progress.entrySet()) {
                    Map<String, Object> questsData = new LinkedHashMap<>();

                    for (Map.Entry<String, QuestProgress> questEntry : playerEntry.getValue().entrySet()) {
                        Map<String, Object> questData = new LinkedHashMap<>();
                        
                        // Save counters
                        Map<String, Integer> counters = questEntry.getValue().getAll();
                        if (!counters.isEmpty()) {
                            questData.put("counters", new LinkedHashMap<>(counters));
                        }

                        questsData.put(questEntry.getKey(), questData);
                    }

                    if (!questsData.isEmpty()) {
                        playersData.put(playerEntry.getKey().toString(), questsData);
                    }
                }

                progressConfig.set("players", playersData);
                progressConfig.save();
                
                plugin.getLogger().debug("Quest progress saved for " + progress.size() + " players");

            } catch (Exception e) {
                plugin.getLogger().error("Failed to save quest progress: " + e.getMessage(), e);
            }
        });
    }

    public void reload() {
        load();
        loadProgress();
        plugin.getLogger().info("🔄 Quests reloaded from quests.yml");
    }

    public Quest getQuest(String id) {
        if (id == null) return null;
        return quests.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Quest> getAll() {
        return quests.values();
    }

    public QuestProgress getProgress(Player p, String questId) {
        Map<String, QuestProgress> perPlayer = progress.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        return perPlayer.computeIfAbsent(questId.toLowerCase(Locale.ROOT), k -> new QuestProgress());
    }

    /**
     * Get all active quests for a player
     */
    public Map<String, QuestProgress> getPlayerProgress(UUID playerId) {
        return progress.getOrDefault(playerId, new HashMap<>());
    }

    /** Try to complete quest; if done, pay reward and remove progress entry */
    public boolean completeIfReady(Player p, Quest q) {
        QuestProgress prog = getProgress(p, q.getId());
        if (q.isComplete(prog)) {
            plugin.getEconomyManager().addBalance(p.getUniqueId(), (int) q.getRewardCoins());
            p.sendMessage("§aQuest complete! Reward: §e" + q.getRewardCoins() + " coins");
            p.sendTitle("§6Quest Complete!", "§e" + q.getName(), 10, 60, 10);
            
            // Remove completed quest
            Map<String, QuestProgress> perPlayer = progress.get(p.getUniqueId());
            if (perPlayer != null) {
                perPlayer.remove(q.getId().toLowerCase(Locale.ROOT));
            }
            
            // Save immediately after completion
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
        Map<String, QuestProgress> perPlayer = progress.get(killer.getUniqueId());
        if (perPlayer == null || perPlayer.isEmpty()) return;

        boolean progressMade = false;

        for (Map.Entry<String, QuestProgress> entry : perPlayer.entrySet()) {
            Quest q = quests.get(entry.getKey());
            if (q == null) continue;

            for (Objective o : q.getObjectives()) {
                if (o instanceof KillObjective) {
                    KillObjective ko = (KillObjective) o;
                    o.onKill(killer, "zombie", entry.getValue());
                    progressMade = true;
                }
            }
            
            completeIfReady(killer, q);
        }

        // Save progress after kills (throttled by auto-save)
        if (progressMade) {
            // Don't save on every kill - rely on auto-save and quit handler
        }
    }

    // NEW: Save progress when player joins (in case of data)
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Progress will be loaded from file automatically
        UUID id = e.getPlayer().getUniqueId();
        if (progress.containsKey(id)) {
            int activeQuests = progress.get(id).size();
            if (activeQuests > 0) {
                e.getPlayer().sendMessage("§7You have §e" + activeQuests + " §7active quest(s).");
            }
        }
    }

    // NEW: Save progress when player quits
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Save immediately on quit to prevent data loss
        saveProgress();
    }
}
