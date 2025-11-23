package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.mob.EntityZombie;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

import java.util.*;

public class InfectionManager implements Listener {
    private final ZombieLooterX plugin;
    private int infectionLevel = 0; // 0..100

    private boolean outbreakActive = false;
    private long outbreakEndMs = 0L;
    private Level spawnLevel;
    private final Set<Long> activeZombieIds = new HashSet<>();
    private final Map<UUID, Integer> killCounts = new HashMap<>();
    private Set<UUID> participants = new HashSet<>();
    private static final long OUTBREAK_DURATION_MS = 5 * 60 * 1000L;
    private static final int MAX_TOTAL_ZOMBIES = 50;

    public InfectionManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startLoop();
    }

    private void startLoop() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new Task() {
            @Override
            public void onRun(int currentTick) {
                for (Level level : plugin.getServer().getLevels().values()) {
                    long t = level.getTime() % 24000;
                    boolean isNight = t >= 13000 && t <= 23000;
                    if (isNight) increase(2);
                    else decrease(1);
                }

                if (infectionLevel >= 100 && !outbreakActive) {
                    startOutbreak();
                    infectionLevel = 60; // cool down
                }

                if (outbreakActive && System.currentTimeMillis() > outbreakEndMs) {
                    endOutbreak("time");
                }
            }
        }, 20 * 20); // every 20s
    }

    public void increase(int n) { infectionLevel = Math.min(100, infectionLevel + n); }
    public void decrease(int n) { infectionLevel = Math.max(0, infectionLevel - n); }
    public int getInfectionLevel() { return infectionLevel; }
    public boolean isOutbreakActive() { return outbreakActive; }
    public boolean isSpawnWorld(Level level) {
        return spawnLevel != null && level != null && spawnLevel.getName().equalsIgnoreCase(level.getName());
    }

    private void startOutbreak() {
        spawnLevel = plugin.getServer().getLevelByName(plugin.getWorldPortalManager().getSpawnWorld());
        if (spawnLevel == null) {
            plugin.getLogger().warning("[Infection] Cannot start outbreak: spawn world not loaded.");
            return;
        }

        participants = new HashSet<>();
        for (Player p : spawnLevel.getPlayers().values()) {
            participants.add(p.getUniqueId());
        }
        killCounts.clear();
        activeZombieIds.clear();

        outbreakActive = true;
        outbreakEndMs = System.currentTimeMillis() + OUTBREAK_DURATION_MS;

        spawnOutbreakWave();
        plugin.getServer().broadcastMessage(TextFormat.colorize('&',
                "&4☣ Infection Outbreak! Zombies swarm the spawn area. Survive and fight them off!"));
    }

    private void spawnOutbreakWave() {
        if (spawnLevel == null) return;
        Location center = spawnLevel.getSafeSpawn();
        int playerCount = Math.max(1, spawnLevel.getPlayers().size());
        int total = Math.max(8, Math.min(MAX_TOTAL_ZOMBIES, playerCount * 4));

        Random rnd = plugin.getServer().getRandom();
        for (int i = 0; i < total; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double dist = 10 + rnd.nextDouble() * 25;
            int x = (int) Math.round(center.getX() + Math.cos(angle) * dist);
            int z = (int) Math.round(center.getZ() + Math.sin(angle) * dist);
            int y = spawnLevel.getHighestBlockAt(x, z) + 1;
            Location loc = new Location(x + 0.5, y, z + 0.5, spawnLevel);

            Entity entity = Entity.createEntity(EntityZombie.NETWORK_ID, loc);
            if (entity instanceof EntityZombie zombie) {
                zombie.setCanClimb(true);
                zombie.setMaxHealth(20);
                zombie.setHealth(20);
                zombie.setNameTagVisible();
                zombie.setNameTag(TextFormat.colorize('&', "&4Infection Minion"));
                if (zombie.namedTag != null) {
                    zombie.namedTag.putBoolean("infectionOutbreak", true);
                }
                zombie.spawnToAll();
                activeZombieIds.add(zombie.getId());
            }
        }
    }

    private void endOutbreak(String reason) {
        outbreakActive = false;
        activeZombieIds.clear();
        plugin.getServer().broadcastMessage(TextFormat.colorize('&', "&2Infection outbreak ended (" + reason + ")."));

        for (UUID uuid : participants) {
            int kills = killCounts.getOrDefault(uuid, 0);
            if (kills <= 0) continue;
            int coins = 5 + kills * 3;
            plugin.getEconomyManager().addBalance(uuid, coins);

            double rareChance = Math.min(0.5, kills * 0.02);
            Item reward = rollReward(rareChance);
            plugin.getServer().getPlayer(uuid).ifPresent(p -> {
                if (reward != null) {
                    p.getInventory().addItem(reward);
                }
                p.sendMessage(TextFormat.colorize('&',
                        "&aOutbreak rewards: &e" + coins + " coins" +
                                (reward != null ? " &7+ &b" + reward.getName() : "")));
            });
        }
        participants.clear();
        killCounts.clear();
    }

    private Item rollReward(double rareChance) {
        double roll = plugin.getServer().getRandom().nextDouble();
        if (roll < rareChance) {
            return Item.get("minecraft:diamond", 0, 2);
        }
        if (roll < rareChance + 0.25) {
            return Item.get("minecraft:golden_apple", 0, 1);
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!outbreakActive) return;
        if (!(event.getEntity() instanceof EntityZombie)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isSpawnWorld(event.getEntity().getLevel())) return;

        if (!participants.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendPopup(TextFormat.colorize('&', "&cYou joined late and cannot damage outbreak zombies."));
        }
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (!outbreakActive) return;
        Entity ent = event.getEntity();
        if (!(ent instanceof EntityZombie)) return;
        if (!isSpawnWorld(ent.getLevel())) return;
        if (ent.namedTag == null || !ent.namedTag.getBoolean("infectionOutbreak")) return;

        activeZombieIds.remove(ent.getId());
        event.setDrops(Collections.emptyList());
        event.setDroppedExp(1);

        EntityDamageByEntityEvent last = ent.getLastDamageCause() instanceof EntityDamageByEntityEvent ? (EntityDamageByEntityEvent) ent.getLastDamageCause() : null;
        if (last != null && last.getDamager() instanceof Player killer && participants.contains(killer.getUniqueId())) {
            killCounts.merge(killer.getUniqueId(), 1, Integer::sum);
            killer.addExperience(3);
        }

        if (activeZombieIds.isEmpty()) {
            endOutbreak("cleared");
        }
    }
}
