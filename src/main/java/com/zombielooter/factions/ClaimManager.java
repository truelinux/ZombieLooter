package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.Task;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

public class ClaimManager {
    private final ZombieLooterX plugin;
    private final Map<String, String> claims = new HashMap<>();
    private final Map<java.util.UUID, Integer> activePreviews = new HashMap<>();
    private Config config;

    public ClaimManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
        startUpkeepTask();
    }

    public void load() {
        try {
            File f = new File(plugin.getDataFolder(), "claims.yml");
            if (!f.exists()) plugin.saveResource("claims.yml", false);
            config = new Config(f, Config.YAML);

            claims.clear();
            Map<String, Object> data = config.getAll();
            for (String key : data.keySet()) {
                Object val = data.get(key);
                if (val instanceof String) claims.put(key, (String) val);
            }
            plugin.getLogger().info("✅ Loaded " + claims.size() + " land claims.");
        } catch (Exception ex) {
            plugin.getLogger().error("❌ Failed to load claims.yml: " + ex.getMessage());
        }
    }

    public void save() {
        config.setAll(new LinkedHashMap<>(claims));
        config.save();
    }

    /** Visual preview for 30s (ground->playerY+10). */
    public void previewClaimChunk(Player player) {
        Level level = player.getLevel();
        int chunkX = player.getChunkX();
        int chunkZ = player.getChunkZ();

        if (activePreviews.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou already have an active preview.");
            return;
        }
        player.sendMessage("§e🔍 Previewing claim border for §f30s§e. Use §a/f claim confirm §eto claim.");

        TaskHandler taskId = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new Task() {
            int ticks = 0;
            @Override public void onRun(int currentTick) {
                if (!player.isOnline()) {
                    plugin.getServer().getScheduler().cancelTask(activePreviews.remove(player.getUniqueId()));
                    return;
                }
                int startX = chunkX * 16, startZ = chunkZ * 16;
                int topY = player.getFloorY() + 10;
                for (int i = 0; i <= 16; i++) {
                    for (int y = 0; y <= 10; y += 2) {
                        level.addParticleEffect(new Vector3(startX + i, topY - y, startZ), ParticleEffect.VILLAGER_HAPPY);
                        level.addParticleEffect(new Vector3(startX + i, topY - y, startZ + 16), ParticleEffect.VILLAGER_HAPPY);
                        level.addParticleEffect(new Vector3(startX, topY - y, startZ + i), ParticleEffect.VILLAGER_HAPPY);
                        level.addParticleEffect(new Vector3(startX + 16, topY - y, startZ + i), ParticleEffect.VILLAGER_HAPPY);
                    }
                }
                ticks += 10;
                if (ticks >= 600) {
                    plugin.getServer().getScheduler().cancelTask(activePreviews.remove(player.getUniqueId()));
                    player.sendMessage("§7⏰ Your claim preview expired. Run §e/f claim preview §7again.");
                }
            }
        }, 10);
        activePreviews.put(player.getUniqueId(), taskId.getTaskId());
    }

    /** Confirm: flatten to lowest floor (air above) and claim. Enforce power-based claim limit. */
    public boolean claimChunk(Faction faction, Player player) {
        Level level = player.getLevel();
        int chunkX = player.getChunkX(), chunkZ = player.getChunkZ();
        String chunkId = level.getFolderName() + ":" + chunkX + ":" + chunkZ;

        // cancel preview
        if (activePreviews.containsKey(player.getUniqueId())) {
            plugin.getServer().getScheduler().cancelTask(activePreviews.remove(player.getUniqueId()));
        }

        if (claims.containsKey(chunkId)) { player.sendMessage("§cThis land is already claimed!"); return false; }

        // enforce claim limit by power (1 claim per power point)
        int owned = getFactionClaims(faction.getName()).size();
        if (owned >= faction.getPower()) {
            player.sendMessage("§cYour faction lacks power for more claims. (§e" + owned + "§7/§e" + faction.getPower() + "§c)");
            return false;
        }

        flattenChunk(level, chunkX, chunkZ);
        claims.put(chunkId, faction.getName());
        save();
        showBorderOnce(level, chunkX, chunkZ, player, ParticleEffect.EXPLOSION_LABTABLE_FIRE);
        player.sendMessage("§a✅ Land flattened and claimed!");
        return true;
    }

    /** Set all blocks above min surface to air, and min surface to grass. */
    private void flattenChunk(Level level, int chunkX, int chunkZ) {
        int startX = chunkX * 16, startZ = chunkZ * 16;
        int minY = 255;
        int topY = 0;
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                minY = Math.min(minY, level.getHighestBlockAt(startX + x, startZ + z));
                topY = Math.max(topY, level.getHighestBlockAt(startX + x, startZ + z));
            }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new Vector3(startX + x, minY, startZ + z), cn.nukkit.block.Block.get(Block.GRASS_BLOCK));
                for (int y = minY + 1; y <= topY; y++)
                    level.setBlock(new Vector3(startX + x, y, startZ + z), cn.nukkit.block.Block.get(cn.nukkit.block.Block.AIR));
            }
        }
    }

    private void showBorderOnce(Level level, int chunkX, int chunkZ, Player player, ParticleEffect particle) {
        int startX = chunkX * 16, startZ = chunkZ * 16, y = player.getFloorY() + 1;
        for (int i = 0; i <= 16; i++) {
            level.addParticleEffect(new Vector3(startX + i, y, startZ), particle);
            level.addParticleEffect(new Vector3(startX + i, y, startZ + 16), particle);
            level.addParticleEffect(new Vector3(startX, y, startZ + i), particle);
            level.addParticleEffect(new Vector3(startX + 16, y, startZ + i), particle);
        }
    }

    public boolean unclaimChunk(Faction faction, Player player) {
        Level level = player.getLevel();
        String chunkId = level.getFolderName() + ":" + player.getChunkX() + ":" + player.getChunkZ();
        if (!claims.containsKey(chunkId)) { player.sendMessage("§cThis area isn’t claimed!"); return false; }
        if (!claims.get(chunkId).equalsIgnoreCase(faction.getName())) {
            player.sendMessage("§cThis land belongs to another faction!"); return false;
        }
        claims.remove(chunkId);
        save();
        showBorderOnce(level, player.getChunkX(), player.getChunkZ(), player, ParticleEffect.BASIC_SMOKE);
        player.sendMessage("§7🏚 Land unclaimed.");
        return true;
    }

    public Set<String> getFactionClaims(String factionName) {
        Set<String> set = new HashSet<>();
        for (Map.Entry<String,String> e : claims.entrySet()) if (e.getValue().equalsIgnoreCase(factionName)) set.add(e.getKey());
        return set;
    }

    public void reload() { load(); }

    /** Hourly upkeep: charge coins per claim; disband claims if bankrupt. */
    private void startUpkeepTask() {
        int costPerClaim = 5; // coins per hour per claim
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new Task() {
            @Override public void onRun(int currentTick) {
                Map<String, Integer> costByFaction = new HashMap<>();
                for (String owner : claims.values()) costByFaction.put(owner, costByFaction.getOrDefault(owner, 0) + costPerClaim);
                for (Map.Entry<String,Integer> e : costByFaction.entrySet()) {
                    com.zombielooter.economy.EconomyManager eco = plugin.getEconomyManager();
                    int total = e.getValue();
                    if (!eco.withdrawFaction(e.getKey(), total)) {
                        plugin.getLogger().warning("Faction " + e.getKey() + " couldn’t pay upkeep. Removing one claim.");
                        // remove one arbitrary claim
                        String toRemove = null;
                        for (Map.Entry<String,String> c : claims.entrySet()) { if (c.getValue().equalsIgnoreCase(e.getKey())) { toRemove = c.getKey(); break; } }
                        if (toRemove != null) claims.remove(toRemove);
                        save();
                    }
                }
            }
        }, 20 * 60 * 60); // every hour
    }

    public Map<String, String> getClaims() { return claims; }
}
