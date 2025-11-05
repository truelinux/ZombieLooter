package com.zombielooter;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityID;
import cn.nukkit.entity.mob.EntityZombie;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;

import java.util.Random;

public class ZombieSpawner implements Listener {

    private final ZombieLooterX plugin;
    private final LootManager lootManager;
    private final Random random = new Random();

    private double hordeSpawnChance;
    private int hordeMinDistance;
    private int hordeMaxDistance;
    private int hordeSizeMin;
    private int hordeSizeMax;
    private int checkInterval;

    public ZombieSpawner(ZombieLooterX plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;

        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startNightHordeTask();
    }

    /**
     * Loads or reloads configuration from config.yml.
     */
    public void loadConfig() {
        plugin.saveResource("config.yml", false);
        Config config = new Config(plugin.getDataFolder() + "/config.yml", Config.YAML);

        hordeSpawnChance = config.getDouble("horde.spawn_chance", 0.07);
        hordeMinDistance = config.getInt("horde.min_distance", 10);
        hordeMaxDistance = config.getInt("horde.max_distance", 20);
        hordeSizeMin = config.getInt("horde.size_min", 3);
        hordeSizeMax = config.getInt("horde.size_max", 7);
        checkInterval = config.getInt("horde.check_interval", 200);

        plugin.getLogger().info("♻ ZombieSpawner config loaded successfully.");
    }

    /**
     * Allows config reload via command.
     */
    public void reloadConfigCommand(CommandSender sender) {
        loadConfig();
        sender.sendMessage("§aZombieLooterX configuration reloaded successfully!");
    }

    /**
     * Periodically checks for night time and spawns hordes with configurable chance.
     */
    private void startNightHordeTask() {
        plugin.getServer().getScheduler().scheduleDelayedRepeatingTask(plugin, new Task() {
            @Override
            public void onRun(int currentTick) {
                for (Player player : plugin.getServer().getOnlinePlayers().values()) {
                    Level level = player.getLevel();
                    if (level == null) continue;

                    long time = level.getTime() % 24000;
                    boolean isNight = (time >= 13000 && time <= 23000);

                    if (isNight && random.nextDouble() <= hordeSpawnChance) {
                        int hordeSize = hordeSizeMin + random.nextInt(Math.max(1, hordeSizeMax - hordeSizeMin + 1));
                        spawnHordeRandom(level, player, hordeSize);

                        // Sound & particle feedback
                        level.addSound(player.getPosition(), Sound.MOB_ZOMBIE_SAY, 1, 1);
                        level.addParticleEffect(player.getPosition(), ParticleEffect.CAMPFIRE_SMOKE_TALL);
                        player.sendTitle("§c⚠ HORDE APPROACHING ⚠", "§7You hear screams in the dark...", 10, 60, 10);
                        plugin.getLogger().info("🌙 Horde spawned near " + player.getName());
                    }
                }
            }
        }, checkInterval, checkInterval);
    }

    /**
     * Spawns a single zombie with an optional custom name.
     */
    public void spawnZombie(Level level, Vector3 position, String name) {
        if (level == null || position == null) return;

        IChunk chunk = level.getChunk((int) position.getX() >> 4, (int) position.getZ() >> 4);
        if (chunk == null || !chunk.isGenerated()) return;

        Location spawnLoc = new Location(position.getX(), position.getY(), position.getZ(), level);
        Entity entity = Entity.createEntity(EntityID.ZOMBIE, spawnLoc);

        if (!(entity instanceof EntityZombie zombie)) return;

        if (name != null && !name.isEmpty()) {
            zombie.setNameTag(name);
            zombie.setNameTagVisible(true);
        }

        zombie.spawnToAll();
    }

    /**
     * Spawns a horde in a random safe spot near the given location.
     */
    public void spawnHordeRandom(Level level, Vector3 origin, int count) {
        if (level == null || origin == null || count <= 0) return;

        Location safeSpot = findSafeSpot(level, origin, hordeMinDistance, hordeMaxDistance);
        if (safeSpot == null) {
            plugin.getLogger().warning("⚠ No safe spawn location found near " + origin.toString());
            return;
        }

        for (int i = 0; i < count; i++) {
            double offsetX = random.nextDouble() * 4 - 2;
            double offsetZ = random.nextDouble() * 4 - 2;
            Vector3 spawnPos = new Vector3(safeSpot.getX() + offsetX, safeSpot.getY(), safeSpot.getZ() + offsetZ);
            spawnZombie(level, spawnPos, "§2Infected");
        }

        // Horde feedback effects
        level.addSound(safeSpot, Sound.MOB_ZOMBIE_REMEDY, 1, 1);
        level.addParticleEffect(safeSpot, ParticleEffect.EXPLOSION_LABTABLE_FIRE);
        plugin.getLogger().info("Spawned horde of " + count + " zombies near " + safeSpot.toString());
    }

    /**
     * Spawns hordes near all online players — used by InfectionManager during global Purge events.
     * Each player gets a random horde at a safe spot nearby.
     */
    public void spawnHordesNearAll(int minCount, int maxCount) {
        if (plugin.getServer().getOnlinePlayers().isEmpty()) return;

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            Level level = player.getLevel();
            if (level == null) continue;

            int hordeSize = minCount + random.nextInt(Math.max(1, maxCount - minCount + 1));

            // Find a safe location near player
            Location safeSpot = findSafeSpot(level, player, 8, 16);
            if (safeSpot == null) continue;

            for (int i = 0; i < hordeSize; i++) {
                double offsetX = random.nextDouble() * 4 - 2;
                double offsetZ = random.nextDouble() * 4 - 2;
                Vector3 spawnPos = new Vector3(safeSpot.getX() + offsetX, safeSpot.getY(), safeSpot.getZ() + offsetZ);
                spawnZombie(level, spawnPos, "§4Purge Zombie");
            }

            // Effects
            level.addSound(safeSpot, Sound.MOB_ZOMBIE_UNFECT, 1, 1);
            level.addParticleEffect(safeSpot, ParticleEffect.HUGE_EXPLOSION_LEVEL);
            player.sendMessage("§4☣ A purge horde has appeared nearby!");
        }
    }


    /**
     * Finds a safe spawn spot within configured range.
     */
    private Location findSafeSpot(Level level, Vector3 origin, int minDist, int maxDist) {
        for (int i = 0; i < 20; i++) {
            double distance = minDist + random.nextDouble() * (maxDist - minDist);
            double angle = random.nextDouble() * 2 * Math.PI;

            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            int x = (int) (origin.getX() + offsetX);
            int z = (int) (origin.getZ() + offsetZ);
            int y = level.getHighestBlockAt(x, z);

            Block ground = level.getBlock(x, y - 1, z);
            Block air = level.getBlock(x, y, z);

            if (ground.isSolid() && air.getId().equals(Block.AIR)) {
                return new Location(x + 0.5, y, z + 0.5, level);
            }
        }
        return null;
    }

    /**
     * Handles loot drops when a zombie dies.
     */
    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Only handle zombies
        if (!(entity instanceof EntityZombie)) return;

        try {
            lootManager.dropLoot(entity);
        } catch (Exception e) {
            plugin.getLogger().error("Failed to drop loot for zombie death: " + e.getMessage());
        }
    }

}
