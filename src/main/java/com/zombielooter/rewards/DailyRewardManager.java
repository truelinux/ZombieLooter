package com.zombielooter.rewards;

import cn.nukkit.Player;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.fake.FakeInventory;
import cn.nukkit.inventory.fake.FakeInventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.FeedbackUtil;
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
    private final List<WheelEntry> globalWheelEntries;
    private final int maxStreak;
    private final int highestConfiguredDay;
    private final Map<UUID, WheelSpinSession> activeSpins = new HashMap<>();
    private final Random random = new Random();
    private static final int SPIN_WINDOW_SIZE = 9;
    private static final int SPIN_CENTER_SLOT = 4;
    private static final int MIN_SPIN_STEPS = 28;
    private static final int MAX_SPIN_STEPS = 36;
    private static final double START_SPIN_DELAY_SECONDS = 0.09D;
    private static final double MAX_SPIN_DELAY_SECONDS = 0.5D;
    private static final double SPIN_DELAY_GROWTH = 0.015D;
    private static final Item POINTER_GLASS = Item.get("minecraft:stained_glass_pane", 11, 1);

    public DailyRewardManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        plugin.saveResource("daily_rewards.yml", false);
        this.config = new Config(new File(plugin.getDataFolder(), "daily_rewards.yml"), Config.YAML);
        this.dataConfig = new Config(new File(plugin.getDataFolder(), "daily_rewards_data.yml"), Config.YAML);
        Map<String, Object> root = config.getAll();
        Map<String, Object> section = (Map<String, Object>) root.getOrDefault("daily-rewards", Collections.emptyMap());
        this.maxStreak = Math.max(1, ((Number) section.getOrDefault("streak-cap", 7)).intValue());
        this.globalWheelEntries = loadGlobalWheel(section);
        this.highestConfiguredDay = loadRewards(section);
    }

    private List<WheelEntry> loadGlobalWheel(Map<String, Object> section) {
        Object wheelObj = section.get("wheel");
        List<WheelEntry> entries = parseWheelEntries(wheelObj, 0, Collections.emptyList(), Collections.emptyList(), "");
        if (entries.isEmpty()) {
            plugin.getLogger().warning("[DailyWheel] No global wheel entries configured. Falling back to static rewards.");
        } else {
            plugin.getLogger().info("[DailyWheel] Loaded " + entries.size() + " wheel entries.");
        }
        return Collections.unmodifiableList(entries);
    }

    private int loadRewards(Map<String, Object> section) {
        Map<String, Object> rewardSection = (Map<String, Object>) section.getOrDefault("rewards", Collections.emptyMap());
        int highestDay = 1;

        for (Map.Entry<String, Object> entry : rewardSection.entrySet()) {
            try {
                int day = Integer.parseInt(entry.getKey());
                if (day < 1) continue;
                Map<String, Object> rewardData = (Map<String, Object>) entry.getValue();
                int money = ((Number) rewardData.getOrDefault("money", 0)).intValue();
                List<Item> items = parseItems((List<String>) rewardData.getOrDefault("items", Collections.emptyList()));
                List<String> commands = new ArrayList<>((List<String>) rewardData.getOrDefault("commands", Collections.emptyList()));
                String message = (String) rewardData.getOrDefault("message", "");
                DayReward reward = new DayReward(money, items, commands, message, globalWheelEntries);
                rewards.put(day, reward);
                if (globalWheelEntries.isEmpty()) {
                    debugWheelEntries(day, reward);
                }
                highestDay = Math.max(highestDay, day);
            } catch (NumberFormatException ignored) {
            }
        }
        return highestDay;
    }

    private List<Item> parseItems(List<String> entries) {
        List<Item> items = new ArrayList<>();
        if (entries == null) {
            return items;
        }
        for (String line : entries) {
            Item item = parseItem(line);
            if (item != null && item.getFullId() != 0) {
                items.add(item);
            }
        }
        return items;
    }

    private List<Item> cloneItems(List<Item> source) {
        List<Item> results = new ArrayList<>();
        if (source == null) {
            return results;
        }
        for (Item item : source) {
            if (item == null) continue;
            results.add(item.clone());
        }
        return results;
    }

    private List<WheelEntry> parseWheelEntries(Object wheelObj, int fallbackMoney, List<Item> fallbackItems, List<String> fallbackCommands, String fallbackMessage) {
        if (!(wheelObj instanceof List<?> rawList)) {
            return Collections.emptyList();
        }
        List<WheelEntry> entries = new ArrayList<>();
        for (Object obj : rawList) {
            if (!(obj instanceof Map<?, ?> map)) continue;
            WheelEntry entry = buildWheelEntry((Map<String, Object>) map, fallbackMoney, fallbackItems, fallbackCommands, fallbackMessage);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private void debugWheelEntries(int day, DayReward reward) {
        if (reward == null) return;
        plugin.getLogger().debug("[DailyWheel] Loaded day " + day + " wheel entries: " + reward.describeEntries());
    }

    private WheelEntry buildWheelEntry(Map<String, Object> data, int fallbackMoney, List<Item> fallbackItems, List<String> fallbackCommands, String fallbackMessage) {
        int weight = Math.max(1, ((Number) data.getOrDefault("weight", 1)).intValue());
        int money = ((Number) data.getOrDefault("money", fallbackMoney)).intValue();
        List<Item> items = parseItems((List<String>) data.getOrDefault("items", Collections.emptyList()));
        if (items.isEmpty()) {
            items = cloneItems(fallbackItems);
        }
        List<String> commands;
        Object cmdValue = data.get("commands");
        if (cmdValue instanceof List<?> list) {
            commands = new ArrayList<>();
            for (Object c : list) {
                if (c != null) commands.add(c.toString());
            }
        } else {
            commands = new ArrayList<>(fallbackCommands);
        }
        String message = (String) data.getOrDefault("message", fallbackMessage);
        String title = (String) data.getOrDefault("title", data.getOrDefault("name", ""));
        String iconString = (String) data.getOrDefault("icon", data.getOrDefault("display", ""));
        Item icon = parseItem(iconString);
        if (icon == null || icon.getFullId() == 0) {
            if (!items.isEmpty()) {
                icon = items.get(0).clone();
                icon.setCount(1);
            } else {
                icon = Item.get("minecraft:paper");
            }
        } else {
            icon.setCount(1);
        }
        return new WheelEntry(weight, money, items, commands, message, title, icon);
    }

    public void claimDailyReward(Player player) {
        UUID playerId = player.getUniqueId();
        RewardState state = loadRewardState(playerId);
        LocalDate today = LocalDate.now();

        if (state.lastDate != null && state.lastDate.equals(today)) {
            player.sendMessage(TextFormat.colorize('&', getMessage("messages.already_claimed", "&cYou have already claimed today's reward.")));
            return;
        }

        int streak;
        if (state.lastDate != null && state.lastDate.plusDays(1).equals(today)) {
            streak = state.streak + 1;
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

        grantDayReward(player, reward, streak);
        saveRewardState(playerId, streak);
    }

    public void spinDailyWheel(Player player) {
        UUID playerId = player.getUniqueId();
        if (activeSpins.containsKey(playerId)) {
            player.sendMessage(TextFormat.colorize('&', getMessage("messages.spin_in_progress", "&eYour daily wheel is already spinning.")));
            return;
        }
        if (globalWheelEntries.isEmpty()) {
            player.sendMessage(TextFormat.colorize('&', "&cDaily spin is not configured."));
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate lastSpin = loadSpinDate(playerId);
        if (lastSpin != null && lastSpin.equals(today)) {
            player.sendMessage(TextFormat.colorize('&', getMessage("messages.spin_already", "&cYou already used today's spin.")));
            return;
        }
        player.sendMessage(TextFormat.colorize('&', getMessage("messages.spinning", "&eSpinning the daily wheel...")));
        RewardState rewardState = loadRewardState(playerId);
        DayReward spinSeed = new DayReward(0, Collections.emptyList(), Collections.emptyList(), "", globalWheelEntries);
        startWheelSpin(player, spinSeed, rewardState.streak);
    }

    private void startWheelSpin(Player player, DayReward reward, int streak) {
        WheelEntry winningEntry = reward.pickWinningEntry(random);
        WheelSpinSession session = new WheelSpinSession(player, streak, reward, winningEntry);
        activeSpins.put(player.getUniqueId(), session);
        session.start();
    }

    private List<WheelEntry> buildSpinSpool(DayReward reward, WheelEntry winner, int totalSteps) {
        int spoolSize = totalSteps + SPIN_WINDOW_SIZE;
        List<WheelEntry> spool = new ArrayList<>(spoolSize);
        for (int i = 0; i < spoolSize; i++) {
            spool.add(reward.randomDisplayEntry(random));
        }
        int winnerIndex = Math.min(spoolSize - 1, totalSteps + SPIN_CENTER_SLOT);
        spool.set(winnerIndex, winner);
        return spool;
    }

    private void grantWheelReward(Player player, WheelEntry entry, int streak) {
        if (entry.getMoney() > 0) {
            plugin.getEconomyManager().addBalance(player.getUniqueId(), entry.getMoney());
        }

        List<Item> rewardItems = entry.copyItems();
        if (!rewardItems.isEmpty()) {
            if (!player.isOnline()) {
                for (Item item : rewardItems) {
                    plugin.getMailManager().addMail(player.getUniqueId(), item);
                }
            } else {
                Inventory inv = player.getInventory();
                Item[] leftovers = inv.addItem(rewardItems.toArray(new Item[0]));
                if (leftovers.length > 0 && player.getLevel() != null) {
                    for (Item drop : leftovers) {
                        player.getLevel().dropItem(player, drop);
                    }
                }
            }
        }

        for (String cmd : entry.getCommands()) {
            String parsed = cmd.replace("{player}", player.getName());
            plugin.getServer().executeCommand(plugin.getServer().getConsoleSender(), parsed);
        }

        sendRewardPopup(player, entry, streak);
    }

    private void grantDayReward(Player player, DayReward reward, int streak) {
        WheelEntry base = reward.getBaseEntry();
        if (base == null) return;

        if (base.getMoney() > 0) {
            plugin.getEconomyManager().addBalance(player.getUniqueId(), base.getMoney());
        }

        List<Item> rewardItems = base.copyItems();
        if (!rewardItems.isEmpty()) {
            Inventory inv = player.getInventory();
            Item[] leftovers = inv.addItem(rewardItems.toArray(new Item[0]));
            if (leftovers.length > 0 && player.getLevel() != null) {
                for (Item drop : leftovers) {
                    player.getLevel().dropItem(player, drop);
                }
            }
        }

        for (String cmd : base.getCommands()) {
            String parsed = cmd.replace("{player}", player.getName());
            plugin.getServer().executeCommand(plugin.getServer().getConsoleSender(), parsed);
        }

        String msg = base.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = getMessage("messages.claimed", "&aDaily reward unlocked: &f{reward} &7(+{coins} coins)");
        }
        msg = msg.replace("{day}", String.valueOf(streak))
                .replace("{coins}", String.valueOf(base.getMoney()))
                .replace("{reward}", base.getDisplayName());
        FeedbackUtil.popup(player, msg);
    }

    private void markSpinComplete(UUID playerId) {
        String base = playerId.toString();
        dataConfig.set(base + ".spin.last-spin", System.currentTimeMillis());
        dataConfig.save();
    }

    private class WheelSpinSession {
        private final Player player;
        private final UUID playerId;
        private final WheelEntry winner;
        private final FakeInventory inventory;
        private final List<WheelEntry> spool;
        private final int totalSteps;
        private final int streak;
        private boolean finished = false;

        private WheelSpinSession(Player player, int streak, DayReward rewardTier, WheelEntry winner) {
            this.player = player;
            this.playerId = player.getUniqueId();
            this.streak = streak;
            this.winner = winner;
            this.totalSteps = MIN_SPIN_STEPS + random.nextInt(Math.max(1, (MAX_SPIN_STEPS - MIN_SPIN_STEPS) + 1));
            this.spool = buildSpinSpool(rewardTier, winner, totalSteps);
            String title = TextFormat.colorize('&', getMessage("messages.wheel_title", "&lDaily Wheel"));
            this.inventory = new FakeInventory(FakeInventoryType.CHEST, title);
            this.inventory.setDefaultItemHandler((inv, slot, oldItem, newItem, event) -> event.setCancelled());
        }

        private void start() {
            plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                if (!player.isOnline()) {
                    finish();
                    return;
                }
                player.addWindow(inventory);
                plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                    if (!player.isOnline()) {
                        finish();
                        return;
                    }
                    refreshWindow(0, false);
                    scheduleSteps();
                }, 2);
            }, 5);
        }

        private void scheduleSteps() {
            double delaySeconds = START_SPIN_DELAY_SECONDS;
            int accumulatedTicks = 0;
            for (int step = 1; step <= totalSteps; step++) {
                accumulatedTicks += secondsToTicks(delaySeconds);
                final int current = step;
                int runAt = accumulatedTicks;
                plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> step(current), runAt);
                double growth = SPIN_DELAY_GROWTH + (step / 12.0D) * 0.01D;
                delaySeconds = Math.min(MAX_SPIN_DELAY_SECONDS, delaySeconds + growth);
            }
            plugin.getServer().getScheduler().scheduleDelayedTask(plugin, this::finish, accumulatedTicks + secondsToTicks(0.75D));
        }

        private void step(int index) {
            if (finished) return;
            refreshWindow(index, index >= totalSteps);
            playSpinSound(index >= totalSteps);
        }

        private void refreshWindow(int offset, boolean finalStop) {
            fillBorder();
            int base = 9;
            for (int slot = 0; slot < SPIN_WINDOW_SIZE; slot++) {
                WheelEntry entry = spool.get(offset + slot);
                boolean pointer = slot == SPIN_CENTER_SLOT;
                Item icon = entry.buildDisplayItem(pointer, finalStop && pointer);
                inventory.setItem(base + slot, icon);
            }
            inventory.setItem(base + SPIN_CENTER_SLOT - 1, buildBorderPane(false));
            inventory.setItem(base + SPIN_CENTER_SLOT + 1, buildBorderPane(true));
            if (player.isOnline()) {
                inventory.sendContents(player);
            }
        }

        private void fillBorder() {
            Item pane = buildBlankPane();
            for (int i = 0; i < inventory.getSize(); i++) {
                if (i < 9 || i >= 18 || i == 9 || i == 17) {
                    inventory.setItem(i, pane.clone());
                }
            }
        }

        private Item buildBorderPane(boolean right) {
            Item pane = POINTER_GLASS.clone();
            String arrow = right ? "&6⟶" : "&6⟵";
            pane.setCustomName(TextFormat.colorize('&', arrow + " &eReward " + (right ? "&6⟶" : "&6⟵")));
            pane.setLore(new String[]{
                    TextFormat.colorize('&', "&7Spinning..."),
                    TextFormat.colorize('&', "&7Watch the center slot!")
            });
            return pane;
        }

        private Item buildBlankPane() {
            Item pane = POINTER_GLASS.clone();
            pane.setCustomName(TextFormat.colorize('&', "&7"));
            pane.setLore(new String[0]);
            return pane;
        }

        private void playSpinSound(boolean finalStop) {
            if (player.getLevel() == null) return;
            if (finalStop) {
                player.getLevel().addSound(player, Sound.RANDOM_LEVELUP);
            } else {
                player.getLevel().addSound(player, Sound.CLICK_ON_NETHER_WOOD_BUTTON);
            }
        }

        private void finish() {
            if (finished) return;
            finished = true;
            try {
                grantWheelReward(player, winner, streak);
                markSpinComplete(playerId);
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            activeSpins.remove(playerId);
            if (player.isOnline()) {
                player.removeWindow(inventory);
            }
        }
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
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
        if (item == null || item.getId().equals(Item.AIR.getId())) {
            plugin.getLogger().warning("[DailyWheel] Could not parse item string '" + value + "'");
        } else {
            plugin.getLogger().debug("[DailyWheel] Parsed item '" + value + "' -> " + item.getId() + ":" + item.getDamage());
        }
        return item;
    }

    private String getMessage(String path, String def) {
        Map<String, Object> section = (Map<String, Object>) config.get("messages");
        if (section == null) return def;
        Object value = section.get(path.substring(path.indexOf('.') + 1));
        return value instanceof String ? (String) value : def;
    }

    private RewardState loadRewardState(UUID playerId) {
        String base = playerId.toString();
        long last = dataConfig.getLong(base + ".reward.last-claim", dataConfig.getLong(base + ".last-claim", 0));
        int streak = dataConfig.getInt(base + ".reward.streak", dataConfig.getInt(base + ".streak", 0));
        return new RewardState(toLocalDate(last), streak);
    }

    private void saveRewardState(UUID playerId, int streak) {
        String base = playerId.toString();
        dataConfig.set(base + ".reward.streak", streak);
        dataConfig.set(base + ".reward.last-claim", System.currentTimeMillis());
        dataConfig.save();
    }

    private LocalDate loadSpinDate(UUID playerId) {
        String base = playerId.toString();
        long last = dataConfig.getLong(base + ".spin.last-spin", 0);
        return toLocalDate(last);
    }

    private LocalDate toLocalDate(long timestamp) {
        if (timestamp <= 0) return null;
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static class RewardState {
        private final LocalDate lastDate;
        private final int streak;

        private RewardState(LocalDate lastDate, int streak) {
            this.lastDate = lastDate;
            this.streak = streak;
        }
    }

    private static class DayReward {
        private final WheelEntry fallbackEntry;
        private final List<WheelEntry> wheelEntries;

        private DayReward(int money, List<Item> items, List<String> commands, String message, List<WheelEntry> wheelEntries) {
            Item baseIcon;
            if (items.isEmpty()) {
                baseIcon = Item.get("minecraft:paper");
            } else {
                baseIcon = items.get(0).clone();
                baseIcon.setCount(1);
            }
            this.fallbackEntry = new WheelEntry(1, money, items, commands, message, "", baseIcon);
            this.wheelEntries = wheelEntries == null ? Collections.emptyList() : wheelEntries;
        }

        private WheelEntry pickWinningEntry(Random random) {
            if (wheelEntries.isEmpty()) {
                return fallbackEntry;
            }
            return pickByWeight(wheelEntries, random, fallbackEntry);
        }

        private WheelEntry randomDisplayEntry(Random random) {
            if (wheelEntries.isEmpty()) {
                return fallbackEntry;
            }
            return pickByWeight(wheelEntries, random, fallbackEntry);
        }

        private static WheelEntry pickByWeight(List<WheelEntry> entries, Random random, WheelEntry fallback) {
            if (entries.isEmpty()) return fallback;
            int total = 0;
            for (WheelEntry entry : entries) {
                total += Math.max(1, entry.getWeight());
            }
            if (total <= 0) return fallback;
            int roll = random.nextInt(total) + 1;
            int running = 0;
            for (WheelEntry entry : entries) {
                running += Math.max(1, entry.getWeight());
                if (roll <= running) {
                    return entry;
                }
            }
            return entries.get(entries.size() - 1);
        }

        private String describeEntries() {
            if (wheelEntries.isEmpty()) {
                return "[fallback only]";
            }
            List<String> desc = new ArrayList<>();
            for (WheelEntry entry : wheelEntries) {
                desc.add(entry.debugSummary());
            }
            return String.join("; ", desc);
        }

        private WheelEntry getBaseEntry() {
            return fallbackEntry;
        }
    }

    private static class WheelEntry {
        private final int weight;
        private final int money;
        private final List<Item> items;
        private final List<String> commands;
        private final String message;
        private final String displayName;
        private final Item icon;

        private WheelEntry(int weight, int money, List<Item> items, List<String> commands, String message, String displayName, Item icon) {
            this.weight = Math.max(1, weight);
            this.money = Math.max(0, money);
            this.items = new ArrayList<>();
            if (items != null) {
                for (Item item : items) {
                    if (item == null) continue;
                    this.items.add(item.clone());
                }
            }
            this.commands = new ArrayList<>();
            if (commands != null) {
                for (String cmd : commands) {
                    if (cmd != null) this.commands.add(cmd);
                }
            }
            this.message = message == null ? "" : message;
            this.displayName = determineDisplayName(displayName);
            this.icon = normalizeIcon(icon, this.items);
        }

        private String determineDisplayName(String custom) {
            if (custom != null && !custom.trim().isEmpty()) {
                return custom;
            }
            if (money > 0 && !items.isEmpty()) {
                return money + " coins + " + formatItem(items.get(0));
            }
            if (!items.isEmpty()) {
                return formatItem(items.get(0));
            }
            if (money > 0) {
                return money + " coins";
            }
            return "Mystery Reward";
        }

        private static String formatItem(Item item) {
            return item.getCount() + "x " + item.getName();
        }

        private static Item normalizeIcon(Item icon, List<Item> items) {
            Item use = icon == null ? Item.AIR : icon.clone();
            if (use.getFullId() == 0) {
                if (!items.isEmpty()) {
                    use = items.get(0).clone();
                    use.setCount(1);
                } else {
                    use = Item.get("minecraft:paper");
                }
            } else {
                use.setCount(1);
            }
            return use;
        }

        private Item buildDisplayItem(boolean pointer, boolean celebrate) {
            Item display = icon.clone();
            display.setCount(1);
            String prefix = celebrate ? "&a&l" : (pointer ? "&e&l" : "&7");
            display.setCustomName(TextFormat.colorize('&', prefix + getDisplayName()));
            List<String> lore = new ArrayList<>();
            if (money > 0) {
                lore.add(TextFormat.colorize('&', "&6+" + money + " coins"));
            }
            for (Item item : items) {
                lore.add(TextFormat.colorize('&', "&f" + item.getCount() + "x " + item.getName()));
            }
            if (!commands.isEmpty()) {
                lore.add(TextFormat.colorize('&', "&7+" + commands.size() + " commands"));
            }
            lore.add(TextFormat.colorize('&', celebrate ? "&aReward unlocked!" : (pointer ? "&7Slowing down..." : "&7Rolling...")));
            display.setLore(lore.toArray(new String[0]));
            return display;
        }

        private List<Item> copyItems() {
            List<Item> copy = new ArrayList<>();
            for (Item item : items) {
                copy.add(item.clone());
            }
            return copy;
        }

        private List<String> getCommands() {
            return new ArrayList<>(commands);
        }

        private String getMessage() {
            return message;
        }

        private String getDisplayName() {
            return displayName;
        }

        private int getWeight() {
            return weight;
        }

        private int getMoney() {
            return money;
        }

        private List<Item> getItems() {
            return new ArrayList<>(items);
        }

        private String debugSummary() {
            return "display='" + displayName + "', icon=" + icon.getId() + ':' + icon.getDamage()
                    + " (" + icon.getName() + "), items=" + items.size() + ", money=" + money;
        }
    }

    private void sendRewardPopup(Player player, WheelEntry entry, int streak) {
        if (player == null || !player.isOnline()) return;
        String template = getMessage("messages.claimed_popup", "&eDay {day}&7: &f{reward} {detail}");
        int coins = entry.getMoney();
        int itemCount = entry.getItems().size();
        String detail;
        if (coins > 0 && itemCount > 0) {
            detail = "&6+" + coins + " coins &7/ &f" + itemCount + " items";
        } else if (coins > 0) {
            detail = "&6+" + coins + " coins";
        } else if (itemCount > 0) {
            detail = "&f" + itemCount + " items";
        } else {
            detail = "&7Lucky spin!";
        }
        String msg = template.replace("{day}", String.valueOf(streak))
                .replace("{reward}", entry.getDisplayName())
                .replace("{detail}", detail);
        FeedbackUtil.popup(player, msg);
    }
}
