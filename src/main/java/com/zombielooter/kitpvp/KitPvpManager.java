package com.zombielooter.kitpvp;

import cn.nukkit.Player;
import cn.nukkit.inventory.fake.FakeInventory;
import cn.nukkit.inventory.fake.FakeInventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Position;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Manages KitPvP sessions, kit definitions, stats and AFK handling.
 */
public class KitPvpManager {

    private final ZombieLooterX plugin;
    private final Map<String, KitDefinition> kits = new HashMap<>();
    private final Map<String, KitChest> kitChests = new HashMap<>();
    private final Map<UUID, PlayerState> savedStates = new HashMap<>();
    private final Map<UUID, String> pendingPreview = new HashMap<>();
    private final Map<UUID, KitStats> stats = new HashMap<>();
    private final Map<UUID, Long> lastActivity = new HashMap<>();

    private String kitWorldName;
    private Location arenaSpawn;
    private Location selectionSpawn;
    private int afkSeconds = 60;

    private final Config statsConfig;

    public KitPvpManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.statsConfig = new Config(new File(plugin.getDataFolder(), "kitpvp_stats.yml"), Config.YAML);
        loadFromConfig();
        loadStoredStats();
        startAfkWatcher();
    }

    private void loadFromConfig() {
        plugin.saveResource("kitpvp.yml", false);
        Config config = new Config(new File(plugin.getDataFolder(), "kitpvp.yml"), Config.YAML);
        Map<String, Object> root = config.getAll();
        Map<String, Object> kitpvp = (Map<String, Object>) root.getOrDefault("kitpvp", Collections.emptyMap());

        kitWorldName = (String) kitpvp.getOrDefault("world", "kitpvp");
        afkSeconds = ((Number) kitpvp.getOrDefault("afk-seconds", 60)).intValue();

        arenaSpawn = parseLocation((Map<String, Object>) kitpvp.get("arena-spawn"));
        selectionSpawn = parseLocation((Map<String, Object>) kitpvp.get("selection-spawn"));

        Map<String, Object> kitSection = (Map<String, Object>) kitpvp.getOrDefault("kits", Collections.emptyMap());
        for (Map.Entry<String, Object> entry : kitSection.entrySet()) {
            Map<String, Object> data = (Map<String, Object>) entry.getValue();
            Item icon = parseItem((String) data.getOrDefault("icon", "minecraft:stone"));
            String display = (String) data.getOrDefault("name", entry.getKey());
            List<String> itemStrings = (List<String>) data.getOrDefault("contents", Collections.emptyList());
            List<Item> contents = itemStrings.stream().map(this::parseItem).collect(Collectors.toList());

            Map<String, Object> armorSection = (Map<String, Object>) data.getOrDefault("armor", Collections.emptyMap());
            Item helmet = parseItem((String) armorSection.getOrDefault("helmet", "minecraft:air"));
            Item chest = parseItem((String) armorSection.getOrDefault("chestplate", "minecraft:air"));
            Item legs = parseItem((String) armorSection.getOrDefault("leggings", "minecraft:air"));
            Item boots = parseItem((String) armorSection.getOrDefault("boots", "minecraft:air"));

            KitDefinition kit = new KitDefinition(entry.getKey(), display, icon, contents, new Item[]{boots, legs, chest, helmet});
            kits.put(entry.getKey(), kit);
        }

        Map<String, Object> chestSection = (Map<String, Object>) kitpvp.getOrDefault("chests", Collections.emptyMap());
        for (Map.Entry<String, Object> entry : chestSection.entrySet()) {
            Map<String, Object> data = (Map<String, Object>) entry.getValue();
            Location location = parseLocation((Map<String, Object>) data, (String) data.getOrDefault("world", kitWorldName));
            double offset = ((Number) data.getOrDefault("hologram-offset", 1.5)).doubleValue();
            kitChests.put(entry.getKey(), new KitChest(entry.getKey(), location, offset));
        }

        ensureWorldLoaded();
        spawnHolograms();
    }

    private void ensureWorldLoaded() {
        if (!plugin.getServer().isLevelLoaded(kitWorldName)) {
            plugin.getServer().loadLevel(kitWorldName);
        }
    }

    private Location parseLocation(Map<String, Object> section) {
        return parseLocation(section, kitWorldName);
    }

    private Location parseLocation(Map<String, Object> section, String worldName) {
        if (!plugin.getServer().isLevelLoaded(worldName)) {
            plugin.getServer().loadLevel(worldName);
        }
        if (section == null) {
            return new Location(0, 64, 0, plugin.getServer().getLevelByName(worldName));
        }
        Level level = plugin.getServer().getLevelByName(worldName);
        double x = ((Number) section.getOrDefault("x", 0)).doubleValue();
        double y = ((Number) section.getOrDefault("y", 64)).doubleValue();
        double z = ((Number) section.getOrDefault("z", 0)).doubleValue();
        return new Location(x, y, z, level);
    }

    private Item parseItem(String value) {
        if (value == null || value.isEmpty()) {
            return Item.get(0);
        }
        String[] parts = value.split(" ");
        Item item = Item.fromString(parts[0]);
        if (parts.length > 1) {
            try {
                item.setCount(Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return item;
    }

    public void markActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isKitWorld(Player player) {
        return player.getLevel() != null && player.getLevel().getName().equalsIgnoreCase(kitWorldName);
    }

    public boolean isPreviewing(Player player) {
        return pendingPreview.containsKey(player.getUniqueId());
    }

    public void openPreview(Player player, KitDefinition kit) {
        markActivity(player);
        FakeInventory inv = new FakeInventory(FakeInventoryType.CHEST, kit.getDisplayName());
        inv.setDefaultItemHandler((inventory, slot, oldItem, newItem, event) -> event.setCancelled());
        inv.setContents(buildPreviewContents(kit));
        player.addWindow(inv);
        pendingPreview.put(player.getUniqueId(), kit.getId());
    }

    private Map<Integer, Item> buildPreviewContents(KitDefinition kit) {
        Map<Integer, Item> items = new HashMap<>();
        int slot = 0;
        for (Item armor : kit.getArmor()) {
            items.put(slot++, armor.clone());
        }
        for (Item content : kit.getContents()) {
            items.put(slot++, content.clone());
        }
        return items;
    }

    public void applyPreviewSelection(Player player) {
        String kitId = pendingPreview.remove(player.getUniqueId());
        if (kitId == null) return;
        KitDefinition kit = kits.get(kitId);
        if (kit == null) return;

        applyKit(player, kit);
        sendToArena(player);
    }

    private void applyKit(Player player, KitDefinition kit) {
        player.getInventory().clearAll();
        Map<Integer, Item> mapped = new HashMap<>();
        int slot = 0;
        for (Item item : kit.getContents()) {
            mapped.put(slot++, item.clone());
        }
        player.getInventory().setContents(mapped);
        player.getInventory().setArmorContents(kit.getArmor());
        player.sendMessage(TextFormat.colorize('&', "&aSelected kit: &f" + kit.getDisplayName()));
    }

    public void sendToArena(Player player) {
        ensureWorldLoaded();
        if (arenaSpawn != null) {
            player.teleport(arenaSpawn);
        }
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setLevel(20);
        markActivity(player);
    }

    public void sendToSelection(Player player) {
        ensureWorldLoaded();
        if (selectionSpawn != null) {
            player.teleport(selectionSpawn);
        }
        player.sendMessage(TextFormat.colorize('&', "&7Returned to kit selection."));
        markActivity(player);
    }

    public boolean join(Player player) {
        if (isKitWorld(player)) {
            player.sendMessage(TextFormat.colorize('&', "&cYou are already in KitPvP."));
            return false;
        }
        savedStates.put(player.getUniqueId(), new PlayerState(player));
        ensureWorldLoaded();
        if (selectionSpawn != null) {
            player.teleport(selectionSpawn);
        }
        player.getInventory().clearAll();
        player.getInventory().setArmorContents(new Item[]{Item.get(0), Item.get(0), Item.get(0), Item.get(0)});
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setLevel(20);
        markActivity(player);
        player.sendMessage(TextFormat.colorize('&', "&aWelcome to KitPvP! Tap a kit chest to preview."));
        showLeaderboard(player);
        return true;
    }

    public void leave(Player player) {
        pendingPreview.remove(player.getUniqueId());
        lastActivity.remove(player.getUniqueId());
        PlayerState state = savedStates.remove(player.getUniqueId());
        if (state != null) {
            state.restore(player);
            player.sendMessage(TextFormat.colorize('&', "&7Restored your items and location."));
        }
    }

    public boolean isParticipant(Player player) {
        return savedStates.containsKey(player.getUniqueId()) || isKitWorld(player);
    }

    public Map<UUID, KitStats> getStats() {
        return stats;
    }

    public void handleExternalTeleport(Player player, Level destination) {
        if (destination != null && destination.getName().equalsIgnoreCase(kitWorldName)) {
            return; // Still inside the KitPvP world
        }
        PlayerState state = savedStates.remove(player.getUniqueId());
        pendingPreview.remove(player.getUniqueId());
        lastActivity.remove(player.getUniqueId());
        if (state != null) {
            state.restore(player, true);
            player.sendMessage(TextFormat.colorize('&', "&7Restored your items after leaving KitPvP."));
        }
    }

    public KitChest findChest(Position position) {
        for (KitChest chest : kitChests.values()) {
            Location loc = chest.getLocation();
            if (loc.getLevel() != null && position.getLevel() != null && loc.getLevel().getName().equalsIgnoreCase(position.getLevel().getName())) {
                if (loc.getFloorX() == position.getFloorX() && loc.getFloorY() == position.getFloorY() && loc.getFloorZ() == position.getFloorZ()) {
                    return chest;
                }
            }
        }
        return null;
    }

    public KitDefinition getKit(String id) {
        return kits.get(id);
    }

    private void spawnHolograms() {
        for (KitChest chest : kitChests.values()) {
            Location loc = chest.getLocation();
            if (loc.getLevel() == null) continue;
            KitDefinition kit = kits.get(chest.getKitId());
            if (kit == null) continue;
            Location textLoc = new Location(loc.getX() + 0.5, loc.getY() + chest.getHologramOffset(), loc.getZ() + 0.5, loc.getLevel());
            FloatingTextParticle particle = new FloatingTextParticle(textLoc, TextFormat.colorize('&', "&b" + kit.getDisplayName()));
            loc.getLevel().addParticle(particle);
        }
    }

    public void handleKill(Player killer, Player victim) {
        KitStats killerStats = stats.computeIfAbsent(killer.getUniqueId(), id -> loadStats(id));
        killerStats.addKill();
        saveStats(killer.getUniqueId(), killerStats);
        killer.sendMessage(TextFormat.colorize('&', "&aKill +1 &7(Kills: " + killerStats.getKills() + ")"));
    }

    public void handleDeath(Player victim) {
        KitStats victimStats = stats.computeIfAbsent(victim.getUniqueId(), id -> loadStats(id));
        victimStats.addDeath();
        saveStats(victim.getUniqueId(), victimStats);
        victim.sendMessage(TextFormat.colorize('&', "&cDeath recorded. K/D: " + String.format(Locale.US, "%.2f", victimStats.getKDR())));
    }

    private KitStats loadStats(UUID id) {
        KitStats kitStats = new KitStats();
        String key = id.toString();
        kitStats.setKills(statsConfig.getInt(key + ".kills", 0));
        kitStats.setDeaths(statsConfig.getInt(key + ".deaths", 0));
        stats.put(id, kitStats);
        return kitStats;
    }

    private void loadStoredStats() {
        for (String key : statsConfig.getKeys()) {
            try {
                UUID id = UUID.fromString(key);
                KitStats cached = loadStats(id);
                stats.put(id, cached);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed keys
            }
        }
    }

    private void saveStats(UUID id, KitStats kitStats) {
        String key = id.toString();
        statsConfig.set(key + ".kills", kitStats.getKills());
        statsConfig.set(key + ".deaths", kitStats.getDeaths());
        statsConfig.save();
    }

    public void showLeaderboard(Player player) {
        List<Map.Entry<UUID, KitStats>> sorted = stats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()))
                .limit(5)
                .collect(Collectors.toList());
        player.sendMessage(TextFormat.colorize('&', "&6--- KitPvP Top Killers ---"));
        if (sorted.isEmpty()) {
            player.sendMessage(TextFormat.colorize('&', "&7No stats yet."));
            return;
        }
        int rank = 1;
        for (Map.Entry<UUID, KitStats> entry : sorted) {
            String name = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
            if (name == null) {
                name = entry.getKey().toString().substring(0, 8);
            }
            player.sendMessage(TextFormat.colorize('&', "&e" + (rank++) + ". &f" + name + " &7- Kills: " + entry.getValue().getKills() + " K/D: " + String.format(Locale.US, "%.2f", entry.getValue().getKDR()))));
        }
        plugin.getLeaderboardManager().showKitTop(player, 5);
    }

    private void startAfkWatcher() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> {
            long now = System.currentTimeMillis();
            for (UUID id : new ArrayList<>(lastActivity.keySet())) {
                Player player = plugin.getServer().getPlayer(id);
                if (player == null || !isKitWorld(player)) continue;
                long last = lastActivity.getOrDefault(id, now);
                if (now - last > afkSeconds * 1000L) {
                    sendToSelection(player);
                }
            }
        }, 20);
    }

    public void triggerDeathEffects(Player victim) {
        if (victim.getLevel() == null) {
            return;
        }
        victim.getLevel().addParticleEffect(victim, ParticleEffect.LARGE_EXPLOSION_LEVEL);
        try {
            ParticleEffect firework = ParticleEffect.valueOf("FIREWORKS_SPARK");
            victim.getLevel().addParticleEffect(victim, firework);
        } catch (IllegalArgumentException ignored) {
            // Older runtimes may not expose FIREWORKS_SPARK; fall back to explosion only.
        }
    }
}
