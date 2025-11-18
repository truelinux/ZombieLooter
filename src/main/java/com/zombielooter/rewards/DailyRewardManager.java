package com.zombielooter.rewards;

import cn.nukkit.Player;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.item.Item;
import com.zombielooter.ZombieLooterX;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class DailyRewardManager {

    private final ZombieLooterX plugin;
    private final Config config;
    private final Config dataConfig;
    private final Map<Integer, DayReward> rewards = new HashMap<>();
    private final int maxStreak;
    private final int highestConfiguredDay;

    public DailyRewardManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        plugin.saveResource("daily_rewards.yml", false);
        this.config = new Config(new File(plugin.getDataFolder(), "daily_rewards.yml"), Config.YAML);
        this.dataConfig = new Config(new File(plugin.getDataFolder(), "daily_rewards_data.yml"), Config.YAML);
        this.maxStreak = Math.max(1, config.getInt("daily-rewards.streak-cap", 7));
        this.highestConfiguredDay = loadRewards();
    }

    private int loadRewards() {
        Map<String, Object> root = config.getAll();
        Map<String, Object> section = (Map<String, Object>) root.getOrDefault("daily-rewards", Collections.emptyMap());
        Map<String, Object> rewardSection = (Map<String, Object>) section.getOrDefault("rewards", Collections.emptyMap());
        int highestDay = 1;

        for (Map.Entry<String, Object> entry : rewardSection.entrySet()) {
            try {
                int day = Integer.parseInt(entry.getKey());
                if (day < 1) continue;
                Map<String, Object> rewardData = (Map<String, Object>) entry.getValue();
                int money = ((Number) rewardData.getOrDefault("money", 0)).intValue();
                List<Item> items = new ArrayList<>();
                List<String> itemStrings = (List<String>) rewardData.getOrDefault("items", Collections.emptyList());
                for (String line : itemStrings) {
                    Item item = parseItem(line);
                    if (item.getFullId() != 0) {
                        items.add(item);
                    }
                }
                List<String> commands = new ArrayList<>((List<String>) rewardData.getOrDefault("commands", Collections.emptyList()));
                String message = (String) rewardData.getOrDefault("message", "");
                rewards.put(day, new DayReward(money, items, commands, message));
                highestDay = Math.max(highestDay, day);
            } catch (NumberFormatException ignored) {
            }
        }
        return highestDay;
    }

    public void claimDailyReward(Player player) {
        LocalDate today = LocalDate.now();
        String base = player.getUniqueId().toString();
        long lastClaimMs = dataConfig.getLong(base + ".last-claim", 0);
        int streak = dataConfig.getInt(base + ".streak", 0);

        LocalDate lastDate = lastClaimMs > 0 ? Instant.ofEpochMilli(lastClaimMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() : null;

        if (lastDate != null && lastDate.equals(today)) {
            player.sendMessage(TextFormat.colorize('&', getMessage("messages.already_claimed", "&cYou have already claimed today's reward.")));
            return;
        }

        if (lastDate != null && lastDate.plusDays(1).equals(today)) {
            streak++;
        } else {
            streak = 1;
        }

        if (streak > maxStreak) {
            streak = maxStreak;
        }

        DayReward reward = rewards.getOrDefault(streak, rewards.get(highestConfiguredDay));
        if (reward == null) {
            player.sendMessage(TextFormat.colorize('&', "&cNo rewards configured."));
            return;
        }

        grantReward(player, reward, streak);

        dataConfig.set(base + ".streak", streak);
        dataConfig.set(base + ".last-claim", System.currentTimeMillis());
        dataConfig.save();
    }

    private void grantReward(Player player, DayReward reward, int streak) {
        if (reward.money > 0) {
            plugin.getEconomyManager().addBalance(player.getUniqueId(), reward.money);
        }

        Inventory inv = player.getInventory();
        Item[] leftover = inv.addItem(reward.items.toArray(new Item[0]));
        if (leftover.length > 0) {
            Arrays.stream(leftover).toList().forEach(drop -> player.getLevel().dropItem(player, drop));
        }

        for (String cmd : reward.commands) {
            String parsed = cmd.replace("{player}", player.getName());
            plugin.getServer().executeCommand(plugin.getServer().getConsoleSender(), parsed);
        }

        String customMsg = reward.message;
        if (customMsg == null || customMsg.isEmpty()) {
            customMsg = getMessage("messages.claimed", "&aClaimed day {day} reward!" + (reward.money > 0 ? " &7(+{coins} coins)" : ""));
        }
        customMsg = customMsg.replace("{day}", String.valueOf(streak))
                .replace("{coins}", String.valueOf(reward.money));
        player.sendMessage(TextFormat.colorize('&', customMsg));
    }

    private Item parseItem(String value) {
        if (value == null || value.isEmpty()) {
            return Item.AIR;
        }
        String[] parts = value.split(" ");
        Item item = Item.get(parts[0]);
        if (parts.length > 1) {
            try {
                item.setCount(Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return item;
    }

    private String getMessage(String path, String def) {
        Map<String, Object> section = (Map<String, Object>) config.get("messages");
        if (section == null) return def;
        Object value = section.get(path.substring(path.indexOf('.') + 1));
        return value instanceof String ? (String) value : def;
    }

    private static class DayReward {
        private final int money;
        private final List<Item> items;
        private final List<String> commands;
        private final String message;

        private DayReward(int money, List<Item> items, List<String> commands, String message) {
            this.money = money;
            this.items = items;
            this.commands = commands;
            this.message = message;
        }
    }
}
