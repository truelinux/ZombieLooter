package com.zombielooter.world;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;

import java.util.*;

/**
 * Captures and reapplies per-world player state (location, inventory, health, food, gamemode).
 * Used to isolate inventories between DeadSpawn and the various wild worlds.
 */
public class PlayerWorldState {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final double yaw;
    private final double pitch;
    private final int gamemode;
    private final double health;
    private final int food;
    private final Map<Integer, Item> contents;
    private final Item[] armor;

    public PlayerWorldState(String worldName, double x, double y, double z, double yaw, double pitch,
                            int gamemode, double health, int food,
                            Map<Integer, Item> contents, Item[] armor) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.gamemode = gamemode;
        this.health = health;
        this.food = food;
        this.contents = contents;
        this.armor = armor;
    }

    public static PlayerWorldState capture(Player player) {
        return new PlayerWorldState(
                player.getLevel().getName(),
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getGamemode(),
                player.getHealth(),
                player.getFoodData().getFood(),
                new HashMap<>(player.getInventory().getContents()),
                player.getInventory().getArmorContents().clone()
        );
    }

    public static PlayerWorldState empty(String worldName, Location fallback, int gamemode) {
        Location loc = fallback != null ? fallback : new Location(0, 64, 0);
        return new PlayerWorldState(
                worldName,
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(),
                gamemode,
                20.0,
                20,
                new HashMap<>(),
                new Item[4]
        );
    }

    public void apply(Player player, Level level, Location fallback, boolean forceAdventure) {
        // Teleport first so inventory updates target the correct world
        Location target = new Location(x, y, z, level);
        target.setYaw((float) yaw);
        target.setPitch((float) pitch);
        if (level != null) {
            player.teleport(target);
        } else if (fallback != null) {
            player.teleport(fallback);
        }

        player.getInventory().clearAll();
        player.getInventory().setContents(contents);
        if (armor != null) {
            player.getInventory().setArmorContents(armor);
        }
        player.getInventory().sendContents(player);

        player.setHealth(Math.max(1f, (float) health));
        player.getFoodData().setFood(food);
        if (forceAdventure) {
            player.setGamemode(Player.ADVENTURE);
        } else {
            player.setGamemode(gamemode);
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        map.put("gamemode", gamemode);
        map.put("health", health);
        map.put("food", food);

        Map<String, Map<String, Object>> items = new LinkedHashMap<>();
        for (Integer key : contents.keySet()) {
            items.put(String.valueOf(key), serializeItem(contents.get(key)));
        }
        map.put("items", items);

        List<Map<String, Object>> armorList = new ArrayList<>();
        if (armor != null) {
            for (Item piece : armor) {
                armorList.add(serializeItem(piece));
            }
        }
        map.put("armor", armorList);
        return map;
    }

    public static PlayerWorldState deserialize(String worldName, Map<String, Object> raw) {
        if (raw == null) return null;
        double x = toDouble(raw.get("x"));
        double y = toDouble(raw.get("y"));
        double z = toDouble(raw.get("z"));
        double yaw = toDouble(raw.get("yaw"));
        double pitch = toDouble(raw.get("pitch"));
        int gm = ((Number) raw.getOrDefault("gamemode", Player.SURVIVAL)).intValue();
        double health = toDouble(raw.getOrDefault("health", 20.0));
        int food = ((Number) raw.getOrDefault("food", 20)).intValue();

        Map<Integer, Item> items = new HashMap<>();
        Object itemsObj = raw.get("items");
        if (itemsObj instanceof Map<?, ?> itemMap) {
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                try {
                    int slot = Integer.parseInt(entry.getKey().toString());
                    Item item = (entry.getValue() instanceof Map<?, ?>)
                            ? deserializeItem(toMap(entry.getValue()))
                            : null;
                    if (item != null) {
                        items.put(slot, item);
                    }
                } catch (Exception ignored) {}
            }
        }

        List<Item> armorPieces = new ArrayList<>();
        Object armorObj = raw.get("armor");
        if (armorObj instanceof List<?> list) {
            for (Object el : list) {
                Item piece = (el instanceof Map<?, ?>) ? deserializeItem(toMap(el)) : null;
                if (piece != null) armorPieces.add(piece);
            }
        }
        Item[] armor = armorPieces.toArray(new Item[0]);

        return new PlayerWorldState(worldName, x, y, z, yaw, pitch, gm, health, food, items, armor);
    }

    private static Map<String, Object> serializeItem(Item item) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (item == null) return map;
        map.put("id", item.getId());
        map.put("meta", item.getDamage());
        map.put("count", item.getCount());
        if (item.hasCustomName()) {
            map.put("name", item.getCustomName());
        }
        if (item.getLore() != null && item.getLore().length > 0) {
            map.put("lore", Arrays.asList(item.getLore()));
        }
        return map;
    }

    private static Item deserializeItem(Map<String, Object> raw) {
        if (raw == null) return null;
        String id =((String)raw.getOrDefault("id", "minecraft:air")).toString();
        int meta = ((Number) raw.getOrDefault("meta", 0)).intValue();
        int count = ((Number) raw.getOrDefault("count", 1)).intValue();
        Item item = Item.get(id, meta, count);
        if (item == null) return null;

        if (raw.containsKey("name")) {
            item.setCustomName(String.valueOf(raw.get("name")));
        }
        Object loreObj = raw.get("lore");
        if (loreObj instanceof List<?> loreList) {
            List<String> lore = new ArrayList<>();
            for (Object line : loreList) {
                lore.add(String.valueOf(line));
            }
            item.setLore(lore.toArray(new String[0]));
        }
        return item;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static Map<String, Object> toMap(Object obj) {
        Map<String, Object> map = new HashMap<>();
        if (!(obj instanceof Map<?, ?> raw)) return map;
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            map.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return map;
    }
}
