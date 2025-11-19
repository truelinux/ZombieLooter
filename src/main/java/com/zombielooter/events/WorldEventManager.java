package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import cn.nukkit.utils.TextFormat;

/**
 * WorldEventManager orchestrates dynamic nighttime events such as raids,
 * meteors and infection outbreaks.  Spawn chances and parameters are
 * configurable via events.yml.  Events are checked once per minute.
 */
public class WorldEventManager {
    private final ZombieLooterX plugin;
    private final Random random = new Random();
    private Config cfg;

    public WorldEventManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        loadConfig();
        scheduleLoop();
    }

    /**
     * Load or reload the events.yml configuration.
     */
    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "events.yml");
        if (!file.exists()) {
            plugin.saveResource("events.yml", false);
        }
        cfg = new Config(file, Config.YAML);
    }

    /**
     * Schedule a repeating task that checks for events every minute.
     */
    private void scheduleLoop() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> {
            for (Level level : plugin.getServer().getLevels().values()) {
                long t = level.getTime() % 24000;
                boolean night = t >= 13000 && t <= 23000;
                if (!night) continue;
                if (cfg.getBoolean("events.raids.enabled", true) &&
                        random.nextDouble() < cfg.getDouble("events.raids.spawn_chance", 0.02)) {
                    triggerRaid(level);
                }
                if (cfg.getBoolean("events.meteor.enabled", true) &&
                        random.nextDouble() < cfg.getDouble("events.meteor.spawn_chance", 0.01)) {
                    dropMeteor(level);
                }
                if (cfg.getBoolean("events.infection.enabled", true) &&
                        random.nextDouble() < cfg.getDouble("events.infection.outbreak_chance", 0.005)) {
                    triggerOutbreak(level);
                }
            }
        }, 20 * 60);
    }

    /**
     * Trigger a raid event by spawning hordes near all players.  Hordes spawn
     * counts are read from the config.  A title and cave sound are played.
     *
     * @param level world to spawn raids in
     */
    private void triggerRaid(Level level) {
        int min = cfg.getInt("events.raids.spawn_count.min", 5);
        int max = cfg.getInt("events.raids.spawn_count.max", 10);
        plugin.getZombieSpawner().spawnHordesNearAll(min, max);
        for (Player p : level.getPlayers().values()) {
            p.sendTitle(TextFormat.colorize('&', "&4Raid Incoming!"), TextFormat.colorize('&', "&7Defend yourself!"), 10, 80, 10);
            level.addSound(p, Sound.AMBIENT_CAVE);
        }
    }

    /**
     * Drop a meteor at a random location within a radius of the world spawn.
     * Creates explosion particles and drops random loot defined in events.yml.
     *
     * @param level world
     */
    @SuppressWarnings("unchecked")
    private void dropMeteor(Level level) {
        int radius = cfg.getInt("events.meteor.spawn_radius", 100);
        Location spawn = level.getSpawnLocation().getLocation();
        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = random.nextDouble() * radius;
        int x = (int) (spawn.getX() + Math.cos(angle) * dist);
        int z = (int) (spawn.getZ() + Math.sin(angle) * dist);
        int y = level.getHighestBlockAt(x, z) + 15;
        for (int i = 0; i < 15; i++) {
            level.addParticleEffect(new Location(x, y - i, z, level), ParticleEffect.LARGE_EXPLOSION_LEVEL);
        }
        level.addSound(new Location(x, y, z, level), Sound.RANDOM_EXPLODE);
        List<Map<String, Object>> loot = (List<Map<String, Object>>) cfg.getList("events.meteor.loot");
        if (loot != null) {
            for (Map<String, Object> e : loot) {
                String itemId = String.valueOf(e.get("item"));
                int minA = ((Number) e.getOrDefault("min", 1)).intValue();
                int maxA = ((Number) e.getOrDefault("max", 1)).intValue();
                double chance = ((Number) e.getOrDefault("chance", 1.0)).doubleValue();
                if (random.nextDouble() <= chance) {
                    int amount = minA + random.nextInt(Math.max(1, maxA - minA + 1));
                    cn.nukkit.item.Item item = cn.nukkit.item.Item.get(itemId);
                    if (item != null) {
                        item.setCount(amount);
                        level.dropItem(new Location(x, level.getHighestBlockAt(x, z), z, level), item);
                    }
                }
            }
        }
        plugin.getServer().broadcastMessage("&6☄ A meteor crashed nearby! Investigate the impact site.");
    }

    /**
     * Trigger an infection outbreak by increasing the infection level and
     * broadcasting a warning.
     *
     * @param level world
     */
    private void triggerOutbreak(Level level) {
        plugin.getInfectionManager().increase(30);
        plugin.getServer().broadcastMessage("&4⚠ Infection outbreak! Infection levels rising rapidly.");
        for (Player p : level.getPlayers().values()) {
            p.sendTitle(TextFormat.colorize('&', "&cInfection Outbreak"), TextFormat.colorize('&', "&7Stay alert!"), 10, 80, 10);
        }
    }
}
