package com.zombielooter.economy;

import cn.nukkit.item.Item;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Vendor {

    private final String id;
    private final String name;
    private final String skin;
    private final int dailyBuyLimit;
    private final int dailySellLimit;
    private final Map<String, VendorItem> items = new HashMap<>();

    public Vendor(String id, String name, String skin, int dailyBuyLimit, int dailySellLimit) {
        this.id = id;
        this.name = name;
        this.skin = skin;
        this.dailyBuyLimit = dailyBuyLimit;
        this.dailySellLimit = dailySellLimit;
    }

    public void addItem(VendorItem item) {
        items.put(item.getKey(), item);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSkin() { return skin; }
    public int getDailyBuyLimit() { return dailyBuyLimit; }
    public int getDailySellLimit() { return dailySellLimit; }
    public VendorItem getItem(String key) { return items.get(key); }
    public Set<String> getItemKeys() { return items.keySet(); }

    public static class VendorItem {
        private final String key;
        private final String itemId;
        private final String name;
        private final int sellPrice;
        private final int buyPrice;

        public VendorItem(String key, String itemId, String name, int sellPrice, int buyPrice) {
            this.key = key;
            this.itemId = itemId;
            this.name = name;
            this.sellPrice = sellPrice;
            this.buyPrice = buyPrice;
        }

        public String getKey() { return key; }
        public Item getItem() { return Item.get(itemId); }
        public String getName() { return name; }
        public int getSellPrice() { return sellPrice; }
        public int getBuyPrice() { return buyPrice; }
        public boolean isSellable() { return sellPrice > 0; }
        public boolean isBuyable() { return buyPrice > 0; }
    }
}
