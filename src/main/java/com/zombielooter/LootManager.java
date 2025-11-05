package com.zombielooter;

import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.utils.Config;
import cn.nukkit.entity.Entity;
import cn.nukkit.Player;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class LootManager {

    private final ZombieLooterX plugin;
    private final Config lootConfig;
    private final Random random = new Random();

    public LootManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.lootConfig = new Config(plugin.getDataFolder() + "/zombie_loot.yml", Config.YAML);
    }

    public void dropLoot(Entity entity) {
        if (entity == null || entity.getLevel() == null) return;
        Level level = entity.getLevel();

        String rarity = getRandomRarity();
        List<Map<String, Object>> lootList = (List<Map<String, Object>>) lootConfig.getList("loot." + rarity);

        if (lootList == null || lootList.isEmpty()) return;

        for (Map<String, Object> loot : lootList) {
            double chance = ((Number) loot.getOrDefault("chance", 0.0)).doubleValue();
            if (random.nextDouble() > chance) continue;

            String itemId = (String) loot.getOrDefault("item", "minecraft:rotten_flesh");
            int min = ((Number) loot.getOrDefault("min", 1)).intValue();
            int max = ((Number) loot.getOrDefault("max", 1)).intValue();
            int amount = min + random.nextInt(Math.max(1, max - min + 1));

            Item item = Item.get(itemId);
            if (item == null) continue;
            item.setCount(amount);

            level.dropItem(entity, item);
        }

        plugin.getLogger().info("💀 Dropped loot for entity: " + entity.getName());
    }

    private String getRandomRarity() {
        double roll = random.nextDouble();
        if (roll <= 0.1) return "rare";
        return "common";
    }
}
