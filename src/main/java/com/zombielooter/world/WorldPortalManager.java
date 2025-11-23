package com.zombielooter.world;

import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

public class WorldPortalManager {

    private final ZombieLooterX plugin;
    private final Random random = new Random();
    private Config worldConfig;
    private Config stateConfig;

    private final Map<UUID, Map<String, PlayerWorldState>> playerStates = new HashMap<>();
    private final Map<String, WorldProfile> worldProfiles = new HashMap<>();
    private final List<PortalDefinition> portals = new ArrayList<>();
    private final Map<UUID, Long> portalCooldown = new HashMap<>();

    private static final long PORTAL_COOLDOWN_MS = 1500;
    private static final int DEFAULT_WILD_MIN = 256;
    private static final int DEFAULT_WILD_MAX = 1024;

    private String spawnWorld = "DeadSpawn";
    private boolean forceAdventureAtSpawn = true;

    public WorldPortalManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        loadConfig();
        loadStates();
    }

    public void reload() {
        loadConfig();
        loadStates();
    }

    public boolean isWildWorld(Level level) {
        return level != null && isWildWorld(level.getName());
    }

    public boolean isWildWorld(String worldName) {
        WorldProfile profile = worldProfiles.get(worldName.toLowerCase(Locale.ROOT));
        return profile != null && profile.isWild;
    }

    public String getSpawnWorld() {
        return spawnWorld;
    }

    public void saveAll() {
        persistStates();
    }

    public void snapshotPlayer(Player player) {
        savePlayerState(player, player.getLevel() != null ? player.getLevel().getName() : spawnWorld);
    }

    public void handleJoin(Player player) {
        Level spawnLevel = ensureLevel(spawnWorld);
        if (spawnLevel == null) {
            plugin.getLogger().warning("Spawn world '" + spawnWorld + "' failed to load; using default world.");
            spawnLevel = plugin.getServer().getDefaultLevel();
        }
        Location spawnLoc = spawnLevel != null ? spawnLevel.getSafeSpawn().getLocation() : player.getLocation();

        // Drop the player at DeadSpawn every time
        applyWorldState(player, spawnLevel, spawnWorld, spawnLoc, true, true);
        player.sendMessage(TextFormat.colorize('&', "&eWelcome to &a" + spawnWorld + "&e. Use portals to continue your adventure."));
    }

    public void handleQuit(Player player) {
        savePlayerState(player, player.getLevel() != null ? player.getLevel().getName() : spawnWorld);
        persistStates();
    }

    public void handleMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        if (event.getTo() == null || event.getFrom() == null) return;
        if (event.getFrom().distanceSquared(event.getTo()) < 0.0001) return;
        Player player = event.getPlayer();
        Level level = player.getLevel();
        if (level == null) return;

        for (PortalDefinition portal : portals) {
            if (!portal.sourceWorld.equalsIgnoreCase(level.getName())) continue;
            if (portal.isInside(event.getTo())) {
                if (tryUsePortal(player, portal)) {
                    event.setCancelled(true);
                }
                return;
            }
        }
    }

    private boolean tryUsePortal(Player player, PortalDefinition portal) {
        long now = System.currentTimeMillis();
        long last = portalCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < PORTAL_COOLDOWN_MS) {
            return false;
        }
        portalCooldown.put(player.getUniqueId(), now);

        savePlayerState(player, player.getLevel() != null ? player.getLevel().getName() : spawnWorld);

        Level targetLevel = ensureLevel(portal.targetWorld);
        if (targetLevel == null) {
            player.sendMessage(TextFormat.colorize('&', "&cTarget world '&f" + portal.targetWorld + "&c' is not loaded."));
            return false;
        }

        boolean wildTarget = isWildWorld(portal.targetWorld);
        Location targetLoc = wildTarget
                ? getRandomWildLocation(targetLevel)
                : targetLevel.getSafeSpawn().getLocation();

        applyWorldState(player, targetLevel, portal.targetWorld, targetLoc, wildTarget, false);

        String msg = wildTarget
                ? "&6You slip through the portal into the wilderness..."
                : "&aTeleporting to &f" + portal.targetWorld;
        player.sendMessage(TextFormat.colorize('&', msg));
        return true;
    }

    private void savePlayerState(Player player, String worldName) {
        if (player == null || worldName == null) return;
        playerStates
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(worldName, PlayerWorldState.capture(player));
    }

    private void applyWorldState(Player player, Level targetLevel, String worldName, Location fallback,
                                 boolean defaultToSurvival, boolean forceAdventure) {
        Map<String, PlayerWorldState> perPlayer = playerStates.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        PlayerWorldState state = perPlayer.get(worldName);
        if (state == null) {
            int gm = defaultToSurvival ? Player.SURVIVAL : player.getGamemode();
            state = PlayerWorldState.empty(worldName, fallback, gm);
            perPlayer.put(worldName, state);
        }
        state.apply(player, targetLevel, fallback, forceAdventure || (forceAdventureAtSpawn && worldName.equalsIgnoreCase(spawnWorld)));
        persistStates();
    }

    private Level ensureLevel(String name) {
        if (name == null) return null;
        Level level = plugin.getServer().getLevelByName(name);
        if (level != null) return level;
        plugin.getServer().loadLevel(name);
        return plugin.getServer().getLevelByName(name);
    }

    private Location getRandomWildLocation(Level level) {
        WorldProfile profile = worldProfiles.getOrDefault(level.getName().toLowerCase(Locale.ROOT),
                new WorldProfile(level.getName(), true, DEFAULT_WILD_MIN, DEFAULT_WILD_MAX));
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = profile.minDistance + random.nextDouble() * (profile.maxDistance - profile.minDistance);
            int x = (int) Math.round(Math.cos(angle) * distance);
            int z = (int) Math.round(Math.sin(angle) * distance);
            int bx = x + level.getSpawnLocation().getFloorX();
            int bz = z + level.getSpawnLocation().getFloorZ();
            level.loadChunk(bx, bz);
            int y = level.getHighestBlockAt(bx, bz) + 2;
            if (y <= 0) continue;
            return new Location(bx + 0.5, y, bz + 0.5, level);
        }
        return level.getSafeSpawn().getLocation();
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "worlds.yml");
        if (!file.exists()) {
            plugin.saveResource("worlds.yml", false);
        }
        worldConfig = new Config(file, Config.YAML);

        spawnWorld = worldConfig.getString("spawn-world", spawnWorld);
        forceAdventureAtSpawn = worldConfig.getBoolean("force-adventure-at-spawn", true);
        worldProfiles.clear();
        worldProfiles.put(spawnWorld.toLowerCase(Locale.ROOT), new WorldProfile(spawnWorld, false, 0, 0));

        List<?> wilds = worldConfig.getList("wild-worlds");
        if (wilds != null) {
            for (Object o : wilds) {
                if (!(o instanceof Map<?, ?> entry)) continue;
                String name = String.valueOf(entry.get("name"));
                Object minObj = entry.containsKey("min-distance") ? entry.get("min-distance") : DEFAULT_WILD_MIN;
                Object maxObj = entry.containsKey("max-distance") ? entry.get("max-distance") : DEFAULT_WILD_MAX;
                int min = ((Number) minObj).intValue();
                int max = ((Number) maxObj).intValue();
                worldProfiles.put(name.toLowerCase(Locale.ROOT), new WorldProfile(name, true, min, max));
            }
        }

        portals.clear();
        List<?> portalEntries = worldConfig.getList("portals");
        if (portalEntries != null) {
            for (Object o : portalEntries) {
                if (!(o instanceof Map<?, ?> raw)) continue;
                Map<String, Object> entry = (Map<String, Object>) raw;
                PortalDefinition def = PortalDefinition.from(entry);
                if (def != null) portals.add(def);
            }
        }
    }

    private void loadStates() {
        File file = new File(plugin.getDataFolder(), "world_states.yml");
        if (!file.exists()) {
            stateConfig = new Config(file, Config.YAML);
            return;
        }
        stateConfig = new Config(file, Config.YAML);
        playerStates.clear();

        Object playersObj = stateConfig.get("players");
        if (!(playersObj instanceof Map<?, ?> playersMap)) return;

        for (Map.Entry<?, ?> entry : playersMap.entrySet()) {
            try {
                UUID uuid = UUID.fromString(String.valueOf(entry.getKey()));
                Map<String, Object> worldData = (Map<String, Object>) entry.getValue();
                Map<String, PlayerWorldState> perWorld = new HashMap<>();
                for (Map.Entry<String, Object> worldEntry : worldData.entrySet()) {
                    if (!(worldEntry.getValue() instanceof Map)) continue;
                    PlayerWorldState state = PlayerWorldState.deserialize(worldEntry.getKey(), (Map<String, Object>) worldEntry.getValue());
                    if (state != null) perWorld.put(worldEntry.getKey(), state);
                }
                playerStates.put(uuid, perWorld);
            } catch (Exception ignored) {
            }
        }
    }

    private void persistStates() {
        Map<String, Object> serialized = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, PlayerWorldState>> playerEntry : playerStates.entrySet()) {
            Map<String, Object> worldMap = new LinkedHashMap<>();
            for (Map.Entry<String, PlayerWorldState> worldState : playerEntry.getValue().entrySet()) {
                worldMap.put(worldState.getKey(), worldState.getValue().serialize());
            }
            serialized.put(playerEntry.getKey().toString(), worldMap);
        }
        stateConfig.set("players", serialized);
        stateConfig.save();
    }

    private record PortalDefinition(String id, String sourceWorld, double x, double y, double z,
                                    double radius, String targetWorld) {
        boolean isInside(Location loc) {
            if (loc == null || loc.getLevel() == null) return false;
            if (!loc.getLevel().getName().equalsIgnoreCase(sourceWorld)) return false;
            double dx = loc.getX() - x;
            double dy = loc.getY() - y;
            double dz = loc.getZ() - z;
            return (dx * dx + dy * dy + dz * dz) <= radius * radius;
        }

        static PortalDefinition from(Map<String, Object> map) {
            if (map == null) return null;
            String id = String.valueOf(map.getOrDefault("id", UUID.randomUUID().toString()));
            Map<String, Object> source = (Map<String, Object>) map.get("source");
            if (source == null) return null;
            String sourceWorld = String.valueOf(source.get("world"));
            double x = toDouble(source.get("x"));
            double y = toDouble(source.get("y"));
            double z = toDouble(source.get("z"));
            double radius = toDouble(map.getOrDefault("radius", source.getOrDefault("radius", 2.5)));
            String targetWorld = String.valueOf(map.get("target-world"));
            return new PortalDefinition(id, sourceWorld, x, y, z, radius, targetWorld);
        }
    }

    private record WorldProfile(String name, boolean isWild, int minDistance, int maxDistance) {
    }
}
