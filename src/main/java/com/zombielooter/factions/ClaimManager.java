package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.particle.RedstoneParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cn.nukkit.utils.TextFormat;

public class ClaimManager {

    private final ZombieLooterX plugin;
    private final GUITextManager text;
    private final Config claimsConfig;
    /**
     * Key format: <worldName>:<chunkHash>. This avoids collisions across worlds.
     */
    private final Map<String, String> chunkToFaction = new HashMap<>();
    private final String fallbackWorldName;

    public ClaimManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.text = plugin.getGUITextManager();
        this.claimsConfig = new Config(new File(plugin.getDataFolder(), "claims.yml"), Config.YAML);
        this.fallbackWorldName = plugin.getServer().getDefaultLevel() != null
                ? plugin.getServer().getDefaultLevel().getName()
                : "world";
        loadClaims();
    }

    private void loadClaims() {
        for (String factionName : claimsConfig.getKeys(false)) {
            for (String chunkHash : claimsConfig.getStringList(factionName)) {
                String normalizedKey = normalizeKey(chunkHash);
                chunkToFaction.put(normalizedKey, factionName);
            }
        }
    }

    public void save() {
        Map<String, List<String>> factionToChunks = new HashMap<>();
        for (Map.Entry<String, String> entry : chunkToFaction.entrySet()) {
            factionToChunks
                    .computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }
        claimsConfig.setAll(new LinkedHashMap<String, Object>(factionToChunks));
        claimsConfig.save();
    }

    public int getClaimCount(String factionName) {
        return (int) chunkToFaction.values().stream().filter(name -> name.equals(factionName)).count();
    }

    public void claimChunk(Faction faction, Player player) {
        if (faction == null) return;

        int claimsHeld = getClaimCount(faction.getName());
        int requiredPower = (claimsHeld + 1) * plugin.getPowerManager().getPowerPerClaim();
        if (faction.getPower() < requiredPower) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.no_power", "&cYour faction doesn't have enough power to claim more land!")));
            player.sendMessage(TextFormat.colorize('&', String.format(text.getText("commands.claim.required_power", "&cRequired: %d | Current: %d"), requiredPower, faction.getPower())));
            return;
        }

        String chunkHash = chunkKey(player.getLevel(), player.getChunkX(), player.getChunkZ());
        if (chunkToFaction.containsKey(chunkHash)) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.already_claimed", "&cThis land is already claimed!")));
            return;
        }

        chunkToFaction.put(chunkHash, faction.getName());
        save();
        player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.claim_success", "&aLand claimed successfully!")));
    }

    public void forceClaimChunk(Faction faction, Level level, int chunkX, int chunkZ) {
        String chunkHash = chunkKey(level, chunkX, chunkZ);
        chunkToFaction.put(chunkHash, faction.getName());
        save();
    }

    public void unclaimChunk(Faction faction, Player player) {
        if (faction == null) return;
        String chunkHash = chunkKey(player.getLevel(), player.getChunkX(), player.getChunkZ());
        if (!faction.getName().equals(chunkToFaction.get(chunkHash))) {
            player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.not_your_land", "&cYour faction doesn't own this land!")));
            return;
        }

        chunkToFaction.remove(chunkHash);
        save();
        player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.unclaim_success", "&aLand unclaimed successfully!")));
    }

    public Faction getFactionForChunk(Level level, int chunkX, int chunkZ) {
        String chunkHash = chunkKey(level, chunkX, chunkZ);
        String factionName = chunkToFaction.get(chunkHash);
        return factionName != null ? plugin.getFactionManager().getFaction(factionName) : null;
    }

    public void previewClaimChunk(Player player) {
        int chunkX = player.getChunkX();
        int chunkZ = player.getChunkZ();
        Level level = player.getLevel();

        for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
            for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
                if (x % 2 == 0 || z % 2 == 0) { // Don't render every single block
                    int y = level.getHighestBlockAt(x, z) + 1;
                    level.addParticle(new RedstoneParticle(new Vector3(x, y, z)));
                }
            }
        }
        player.sendMessage(TextFormat.colorize('&', text.getText("commands.claim.preview", "&eDisplaying chunk border for your current location.")));
    }

    /**
     * Find the nearest claim in the same world as the player.
     *
     * @param level  world to search
     * @param chunkX starting chunk X
     * @param chunkZ starting chunk Z
     * @return nearest claim in this world or null if none
     */
    public ClaimLocation findNearestClaim(Level level, int chunkX, int chunkZ) {
        if (level == null) return null;
        String worldName = level.getName();
        ClaimLocation closest = null;
        double bestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, String> entry : chunkToFaction.entrySet()) {
            ClaimLocation loc = parseLocationKey(entry.getKey(), entry.getValue());
            if (loc == null || !worldName.equalsIgnoreCase(loc.world)) continue;

            double dist = distance(chunkX, chunkZ, loc.chunkX, loc.chunkZ);
            if (dist < bestDistance) {
                bestDistance = dist;
                closest = loc;
            }
        }
        return closest;
    }

    public List<ClaimLocation> getAllClaims() {
        List<ClaimLocation> claims = new ArrayList<>();
        for (Map.Entry<String, String> entry : chunkToFaction.entrySet()) {
            ClaimLocation loc = parseLocationKey(entry.getKey(), entry.getValue());
            if (loc != null) {
                claims.add(loc);
            }
        }
        return claims;
    }

    private double distance(int x1, int z1, int x2, int z2) {
        double dx = (x1 - x2);
        double dz = (z1 - z2);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private String chunkKey(Level level, int chunkX, int chunkZ) {
        String worldName = (level != null && level.getName() != null) ? level.getName() : fallbackWorldName;
        return worldName + ":" + Level.chunkHash(chunkX, chunkZ);
    }

    private String normalizeKey(String raw) {
        if (raw == null) return null;
        if (raw.contains(":")) return raw;
        // Legacy entry without world prefix; assume fallback world
        return fallbackWorldName + ":" + raw;
    }

    private ClaimLocation parseLocationKey(String key, String faction) {
        if (key == null) return null;
        String world = fallbackWorldName;
        String hashStr = key;
        int sep = key.indexOf(':');
        if (sep != -1) {
            world = key.substring(0, sep);
            hashStr = key.substring(sep + 1);
        }
        try {
            long hash = Long.parseLong(hashStr);
            int chunkX = (int) (hash & 0xffffffffL);
            int chunkZ = (int) (hash >>> 32);
            return new ClaimLocation(world, faction, chunkX, chunkZ);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static class ClaimLocation {
        public final String world;
        public final String faction;
        public final int chunkX;
        public final int chunkZ;

        public ClaimLocation(String world, String faction, int chunkX, int chunkZ) {
            this.world = world;
            this.faction = faction;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        public int centerBlockX() {
            return chunkX * 16 + 8;
        }

        public int centerBlockZ() {
            return chunkZ * 16 + 8;
        }
    }
}
