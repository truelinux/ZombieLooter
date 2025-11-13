package com.zombielooter.economy;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VendorManager {

    private final ZombieLooterX plugin;
    private final EconomyManager economyManager;
    private final Map<String, Vendor> vendors = new HashMap<>();
    private final Map<UUID, PlayerVendorStats> playerStats = new HashMap<>();

    public VendorManager(ZombieLooterX plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        loadVendors();
    }

    private void loadVendors() {
        File file = new File(plugin.getDataFolder(), "vendors.yml");
        if (!file.exists()) {
            plugin.saveResource("vendors.yml", false);
        }
        Config config = new Config(file, Config.YAML);

        for (String vendorId : config.getSection("vendors").getKeys(false)) {
            String vendorPath = "vendors." + vendorId;
            Vendor vendor = new Vendor(
                vendorId,
                config.getString(vendorPath + ".name"),
                config.getString(vendorPath + ".skin"),
                config.getInt(vendorPath + ".daily-buy-limit"),
                config.getInt(vendorPath + ".daily-sell-limit")
            );

            for (String itemKey : config.getSection(vendorPath + ".items").getKeys(false)) {
                String itemPath = vendorPath + ".items." + itemKey;
                vendor.addItem(new Vendor.VendorItem(
                    itemKey,
                    config.getString(itemPath + ".id"),
                    config.getString(itemPath + ".name"),
                    config.getInt(itemPath + ".sellPrice", -1),
                    config.getInt(itemPath + ".buyPrice", -1)
                ));
            }
            vendors.put(vendorId, vendor);
        }
    }

    public Vendor getVendor(String id) {
        return vendors.get(id);
    }

    public Map<String, Vendor> getVendors() {
        return vendors;
    }

    private PlayerVendorStats getPlayerStats(Player player) {
        PlayerVendorStats stats = playerStats.computeIfAbsent(player.getUniqueId(), k -> new PlayerVendorStats());
        stats.resetIfNewDay();
        return stats;
    }

    public boolean buyItem(Player player, String vendorId, String itemKey, int amount) {
        Vendor vendor = getVendor(vendorId);
        Vendor.VendorItem item = vendor.getItem(itemKey);
        if (item == null || !item.isBuyable()) return false;

        PlayerVendorStats stats = getPlayerStats(player);
        if (stats.getBoughtAmount(vendorId) + amount > vendor.getDailyBuyLimit()) {
            player.sendMessage("§cYou have reached your daily buy limit for this vendor.");
            return false;
        }

        int totalPrice = item.getBuyPrice() * amount;
        if (economyManager.withdraw(player.getUniqueId(), totalPrice)) {
            Item itemToGive = item.getItem();
            itemToGive.setCount(amount);
            player.getInventory().addItem(itemToGive);
            stats.addBoughtAmount(vendorId, amount);
            return true;
        }
        return false;
    }

    public boolean sellItem(Player player, String vendorId, String itemKey, int amount) {
        Vendor vendor = getVendor(vendorId);
        Vendor.VendorItem item = vendor.getItem(itemKey);
        if (item == null || !item.isSellable()) return false;

        PlayerVendorStats stats = getPlayerStats(player);
        if (stats.getSoldAmount(vendorId) + amount > vendor.getDailySellLimit()) {
            player.sendMessage("§cYou have reached your daily sell limit for this vendor.");
            return false;
        }

        Item itemToSell = item.getItem();
        itemToSell.setCount(amount);

        if (player.getInventory().contains(itemToSell)) {
            player.getInventory().removeItem(itemToSell);
            int totalPrice = item.getSellPrice() * amount;
            economyManager.addBalance(player.getUniqueId(), totalPrice);
            stats.addSoldAmount(vendorId, amount);
            return true;
        }
        return false;
    }

    private static class PlayerVendorStats {
        private LocalDate lastResetDay;
        private final Map<String, Integer> boughtAmounts = new HashMap<>();
        private final Map<String, Integer> soldAmounts = new HashMap<>();

        PlayerVendorStats() {
            this.lastResetDay = LocalDate.now();
        }

        void resetIfNewDay() {
            if (!LocalDate.now().equals(lastResetDay)) {
                boughtAmounts.clear();
                soldAmounts.clear();
                lastResetDay = LocalDate.now();
            }
        }

        int getBoughtAmount(String vendorId) {
            return boughtAmounts.getOrDefault(vendorId, 0);
        }

        void addBoughtAmount(String vendorId, int amount) {
            boughtAmounts.put(vendorId, getBoughtAmount(vendorId) + amount);
        }

        int getSoldAmount(String vendorId) {
            return soldAmounts.getOrDefault(vendorId, 0);
        }

        void addSoldAmount(String vendorId, int amount) {
            soldAmounts.put(vendorId, getSoldAmount(vendorId) + amount);
        }
    }
}
