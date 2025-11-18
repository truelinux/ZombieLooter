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
import java.time.Duration;
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
                    if (item.getId() != 0) {
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

    public void showStatus(Player player) {
        String base = player.getUniqueId().toString();
        long lastClaimMs = dataConfig.getLong(base + ".last-claim", 0);
        int streak = dataConfig.getInt(base + ".streak", 0);
        LocalDate today = LocalDate.now();
        LocalDate lastDate = lastClaimMs > 0 ? Instant.ofEpochMilli(lastClaimMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() : null;

        boolean claimedToday = lastDate != null && lastDate.equals(today);
        int nextDay = Math.min(streak + 1, maxStreak);
        DayReward nextReward = rewards.getOrDefault(nextDay, rewards.get(highestConfiguredDay));

        StringBuilder status = new StringBuilder();
        status.append(TextFormat.colorize('&', "&6--- Daily Rewards ---"));
        status.append("\n").append(TextFormat.colorize('&', "&7Current streak: &f" + streak + " day(s)"));
        if (claimedToday) {
            long secondsLeft = secondsUntilTomorrow();
            status.append("\n").append(TextFormat.colorize('&', "&cNext claim in: &f" + formatDuration(secondsLeft)));
        } else {
            status.append("\n").append(TextFormat.colorize('&', "&aYou can claim now!"));
        }
        if (nextReward != null) {
            status.append("\n").append(TextFormat.colorize('&', "&7Next reward (&eDay " + nextDay + "&7): &f" + summarizeReward(nextReward)));
        }
        player.sendMessage(status.toString());
    }

    public void resetProgress(UUID id) {
        String base = id.toString();
        dataConfig.set(base + ".streak", 0);
        dataConfig.set(base + ".last-claim", 0);
        dataConfig.save();
    }

    private void grantReward(Player player, DayReward reward, int streak) {
        if (reward.money > 0) {
            plugin.getEconomyManager().addBalance(player.getUniqueId(), reward.money);
        }

        Inventory inv = player.getInventory();
        for (Item item : reward.items) {
            Map<Integer, Item> leftover = inv.addItem(item.clone());
            if (!leftover.isEmpty()) {
                leftover.values().forEach(drop -> player.getLevel().dropItem(player, drop));
            }
        }

        for (String cmd : reward.commands) {
            String parsed = cmd.replace("{player}", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed);
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

    private String getMessage(String path, String def) {
        Map<String, Object> section = (Map<String, Object>) config.get("messages");
        if (section == null) return def;
        Object value = section.get(path.substring(path.indexOf('.') + 1));
        return value instanceof String ? (String) value : def;
    }

    private long secondsUntilTomorrow() {
        Instant now = Instant.now();
        Instant tomorrow = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        return Math.max(0, Duration.between(now, tomorrow).getSeconds());
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02dh %02dm %02ds", hours, minutes, secs);
    }

    private String summarizeReward(DayReward reward) {
        List<String> parts = new ArrayList<>();
        if (reward.money > 0) {
            parts.add(reward.money + " coins");
        }
        if (!reward.items.isEmpty()) {
            parts.add(reward.items.size() + " item(s)");
        }
        if (!reward.commands.isEmpty()) {
            parts.add("commands x" + reward.commands.size());
        }
        return String.join(", ", parts);
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
