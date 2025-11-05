package com.zombielooter.market;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

public class MarketManager {
    public static class Listing {
        public final UUID seller;
        public final String itemId;
        public final int amount;
        public final int price;

        public Listing(UUID seller, String itemId, int amount, int price) {
            this.seller = seller; this.itemId = itemId; this.amount = amount; this.price = price;
        }
    }

    private final ZombieLooterX plugin;
    private final List<Listing> listings = new ArrayList<>();
    private Config cfg;

    public MarketManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File f = new File(plugin.getDataFolder(), "market.yml");
        if (!f.exists()) plugin.saveResource("market.yml", false);
        cfg = new Config(f, Config.YAML);
        listings.clear();
        List<Map<String,Object>> arr = (List<Map<String,Object>>) cfg.getList("listings", new ArrayList<>());
        for (Map<String,Object> m : arr) {
            try {
                UUID seller = java.util.UUID.fromString(String.valueOf(m.get("seller")));
                String itemId = String.valueOf(m.get("item"));
                int amount = ((Number) m.getOrDefault("amount", 1)).intValue();
                int price = ((Number) m.getOrDefault("price", 1)).intValue();
                listings.add(new Listing(seller, itemId, amount, price));
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        List<Map<String,Object>> arr = new ArrayList<>();
        for (Listing l : listings) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("seller", l.seller.toString());
            m.put("item", l.itemId);
            m.put("amount", l.amount);
            m.put("price", l.price);
            arr.add(m);
        }
        cfg.set("listings", arr); cfg.save();
    }

    public List<Listing> getListings(){ return Collections.unmodifiableList(listings); }

    public void list(Player p, String itemId, int amount, int price) {
        listings.add(new Listing(p.getUniqueId(), itemId, amount, price));
        save();
    }

    public boolean buy(Player buyer, int index) {
        if (index < 0 || index >= listings.size()) return false;
        Listing l = listings.get(index);
        if (!plugin.getEconomyManager().withdraw(buyer.getUniqueId(), l.price)) return false;
        plugin.getEconomyManager().addBalance(l.seller, l.price);
        Item item = Item.get(itemIdToKey(l.itemId));
        if (item == null) return false;
        item.setCount(l.amount);
        buyer.getInventory().addItem(item);
        listings.remove(index);
        save();
        return true;
    }

    private String itemIdToKey(String s) { return s; } // placeholder mapping if needed
}
