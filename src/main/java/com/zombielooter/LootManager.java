package com.zombielooter;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Level;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;

import java.util.*;

public class LootManager {

    private final ZombieLooterX plugin;
    private Config lootConfig;

    private final Random random = new Random();

    public LootManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        plugin.saveResource("loot.yml", false);
        this.lootConfig = new Config(plugin.getDataFolder() + "/loot.yml", Config.YAML);
    }

    public void dropLoot(Entity entity) {
        ConfigSection tiers = lootConfig.getSection("tiers");
        if (tiers == null) return;

        for (String tierName : tiers.getKeys(false)) {
            ConfigSection tier = tiers.getSection(tierName);

            double chance = tier.getDouble("chance", 1.0);
            int rolls = tier.getInt("rolls", 1);

            if (random.nextDouble() <= chance) {
                for (int i = 0; i < rolls; i++) {
                    Item item = getRandomWeightedItem((List<Map<String, Object>>) tier.getList("items"));
                    if (item != null) {
                        entity.level.dropItem(entity.getPosition(), item);
                    }
                }
            }
        }
    }

    private Item getRandomWeightedItem(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return null;

        List<ConfigSection> sections = new ArrayList<>();
        int totalWeight = 0;

        for (Object o : items) {
            ConfigSection s = new ConfigSection((Map<String, Object>) o);
            int weight = s.getInt("weight", 1);
            sections.add(s);
            totalWeight += weight;
        }

        int roll = random.nextInt(totalWeight);
        int current = 0;

        for (ConfigSection sec : sections) {
            current += sec.getInt("weight", 1);
            if (roll < current) {
                return buildItem(sec);
            }
        }

        return null;
    }

    private Item buildItem(ConfigSection section) {
        String id = section.getString("id");
        if (id == null) return null;

        Item item = Item.get(id);
        if (item == null) return null;

        // Amount (supports ranges like 1-4)
        String amountStr = section.getString("amount", "1");
        int amount = parseRange(amountStr);
        item.setCount(amount);

        // Name
        if (section.exists("name")) {
            item.setCustomName(TextFormat.colorize('&', section.getString("name")));
        }

        // Prefix
        if (section.exists("prefixes")) {
            List<String> pfx = section.getStringList("prefixes");
            if (!pfx.isEmpty()) {
                String prefix = pfx.get(random.nextInt(pfx.size()));
                item.setCustomName(TextFormat.colorize('&', prefix + item.getCustomName()));
            }
        }

        // Suffix
        if (section.exists("suffixes")) {
            List<String> sfx = section.getStringList("suffixes");
            if (!sfx.isEmpty()) {
                String suffix = sfx.get(random.nextInt(sfx.size()));
                item.setCustomName(TextFormat.colorize('&', item.getCustomName() + suffix));
            }
        }

        // Lore
        if (section.exists("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(TextFormat.colorize('&', line));
            }
            item.setLore(lore.toArray(new String[0]));
        }

        // Enchantments
        if (section.exists("enchants")) {
            ConfigSection ench = section.getSection("enchants");
            for (String e : ench.getKeys(false)) {

                int lvl;
                Object val = ench.get(e);

                if (val instanceof Map) {
                    ConfigSection range = ench.getSection(e);
                    int min = range.getInt("min", 1);
                    int max = range.getInt("max", 1);
                    lvl = min + random.nextInt((max - min) + 1);
                } else {
                    lvl = ench.getInt(e);
                }

                Enchantment enchantment = Enchantment.getEnchantment(e);
                if (enchantment != null) {
                    item.addEnchantment(enchantment.setLevel(lvl));
                }
            }
        }

        // Custom Model Data
        if (section.exists("custom_model_data")) {
            CompoundTag tag = item.hasCompoundTag() ? item.getNamedTag() : new CompoundTag();
            tag.putInt("CustomModelData", section.getInt("custom_model_data"));
            item.setNamedTag(tag);
        }

        // Generic NBT
        if (section.exists("nbt")) {
            CompoundTag tag = item.hasCompoundTag() ? item.getNamedTag() : new CompoundTag();
            ConfigSection nbt = section.getSection("nbt");

            for (String key : nbt.getKeys(false)) {
                Object val = nbt.get(key);
                if (val instanceof Integer) tag.putInt(key, (int) val);
                else if (val instanceof String) tag.putString(key, (String) val);
                else if (val instanceof Boolean) tag.putBoolean(key, (boolean) val);
                else if (val instanceof Double) tag.putDouble(key, (double) val);
            }

            item.setNamedTag(tag);
        }

        return item;
    }


    private int parseRange(String s) {
        if (s.contains("-")) {
            String[] p = s.split("-");
            int min = Integer.parseInt(p[0]);
            int max = Integer.parseInt(p[1]);
            return min + random.nextInt((max - min) + 1);
        }
        return Integer.parseInt(s);
    }

    public Map<Integer, Item> getAllPossibleLootItems() {
        Map<Integer, Item> items = new HashMap<>();
        ConfigSection tiers = lootConfig.getSection("tiers");
        if (tiers == null) return items;
        Integer slot = 1;
        for (String tierName : tiers.getKeys(false)) {
            ConfigSection tier = tiers.getSection(tierName);
            List<Map<String, Object>> lootList = (List<Map<String, Object>>) tier.getList("items", new ArrayList<>());

            for (Object o : lootList) {
                ConfigSection section = new ConfigSection((Map<String, Object>) o);
                Item item = buildItem(section); // uses your existing item builder
                if (item != null) {
                    if (slot.equals(8) || slot.equals(9) || slot.equals(17) || slot.equals(18)) {
                        slot++;
                        continue;
                    }
                    items.put(slot, item);
                    slot++;
                }
            }
            slot++;
        }

        return items;
    }
}
