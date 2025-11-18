package com.zombielooter.kitpvp;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;

import java.util.Map;

/**
 * PlayerState tracks a player's inventory, armor and core attributes so they can be
 * restored when leaving KitPvP.
 */
public class PlayerState {
    private final String levelName;
    private final Location location;
    private final Map<Integer, Item> inventory;
    private final Item[] armor;
    private final double health;
    private final int food;

    public PlayerState(Player player) {
        this.levelName = player.getLevel() != null ? player.getLevel().getName() : "";
        Location current = player.getLocation();
        this.location = new Location(current.x, current.y, current.z, current.yaw, current.pitch, current.getLevel());
        this.inventory = cloneInventory(player);
        this.armor = cloneArmor(player.getInventory().getArmorContents());
        this.health = player.getHealth();
        this.food = player.getFoodData().getLevel();
    }

    public void restore(Player player) {
        restore(player, false);
    }

    public void restore(Player player, boolean keepCurrentLocation) {
        if (!keepCurrentLocation) {
            if (!player.getServer().isLevelLoaded(levelName)) {
                player.getServer().loadLevel(levelName);
            }
            Level level = player.getServer().getLevelByName(levelName);
            if (level != null) {
                player.teleport(location.setLevel(level));
            }
        }
        player.getInventory().clearAll();
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.setHealth(health);
        player.getFoodData().setLevel(food);
    }

    private Map<Integer, Item> cloneInventory(Player player) {
        Map<Integer, Item> cloned = new java.util.HashMap<>();
        for (Map.Entry<Integer, Item> entry : player.getInventory().getContents().entrySet()) {
            cloned.put(entry.getKey(), entry.getValue().clone());
        }
        return cloned;
    }

    private Item[] cloneArmor(Item[] armorContents) {
        Item[] clone = new Item[armorContents.length];
        for (int i = 0; i < armorContents.length; i++) {
            clone[i] = armorContents[i] == null ? Item.get(0) : armorContents[i].clone();
        }
        return clone;
    }
}
