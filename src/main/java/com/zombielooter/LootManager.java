package com.zombielooter;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LootManager {

    private final ZombieLooterX plugin;
    private final Config lootConfig;
    private final Random random = new Random();
    private final List<LootItem> lootTable = new ArrayList<>();

    private static class LootItem {
        final String itemId;
        final int minAmount;
        final int maxAmount;
        final double chance; // 0.0 to 1.0

        LootItem(String id, int min, int max, double chance) {
            this.itemId = id;
            this.minAmount = min;
            this.maxAmount = max;
            this.chance = chance;
        }
    }

    public LootManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.lootConfig = new Config(plugin.getDataFolder() + "/loot.yml", Config.YAML);
        loadLootTable();
    }

    private void loadLootTable() {
        lootTable.clear();
        // ** THE FIX IS HERE **
        // Add a null check to prevent crashing if the config is empty or malformed.
        List<Map<String, Object>> items = lootConfig.get("zombie_loot", new ArrayList<>());
        if (items == null || items.isEmpty()) {
            plugin.getLogger().warning("The 'zombie_loot' section in loot.yml is missing or empty. No loot will be dropped.");
            return;
        }

        for (Map<String, Object> itemData : items) {
            try {
                lootTable.add(new LootItem(
                    (String) itemData.get("id"),
                    (int) itemData.get("min_amount"),
                    (int) itemData.get("max_amount"),
                    ((Number) itemData.get("chance")).doubleValue()
                ));
            } catch (Exception e) {
                plugin.getLogger().error("Failed to load an item from loot.yml. Please check formatting.", e);
            }
        }
    }

    public void dropLoot(Entity entity) {
        Level level = entity.getLevel();
        Player killer = null;
        if (entity.getLastDamageCause() instanceof cn.nukkit.event.entity.EntityDamageByEntityEvent) {
            Entity damager = ((cn.nukkit.event.entity.EntityDamageByEntityEvent) entity.getLastDamageCause()).getDamager();
            if (damager instanceof Player) {
                killer = (Player) damager;
            }
        }

        double lootBoost = (killer != null) ? plugin.getTerritoryBuffManager().getLootBoost(killer) : 0;

        for (LootItem lootItem : lootTable) {
            double chance = lootItem.chance * (1 + (lootBoost / 100.0));
            if (random.nextDouble() < chance) {
                int amount = lootItem.minAmount + random.nextInt(lootItem.maxAmount - lootItem.minAmount + 1);
                Item item = Item.get(lootItem.itemId, 0, amount);
                level.dropItem(entity.getLocation(), item);
            }
        }
    }
}
