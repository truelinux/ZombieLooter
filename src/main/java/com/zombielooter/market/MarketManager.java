package com.zombielooter.market;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.EconomyManager;
import com.zombielooter.mail.MailManager;

import java.io.File;
import java.util.*;

public class MarketManager {
    public static class Listing {
        public final UUID seller;
        public final String itemId;
        public final int amount;
        public final int price;
        public final Type type;
        public final boolean fake;
        public final long expiresAt;
        public int reserved;

        public Listing(UUID seller, String itemId, int amount, int price, Type type, boolean fake, long expiresAt, int reserved) {
            this.seller = seller; this.itemId = itemId; this.amount = amount; this.price = price; this.type = type; this.fake = fake; this.expiresAt = expiresAt; this.reserved = reserved;
        }
    }

    public enum Type { SELL, BUY }

    private final ZombieLooterX plugin;
    private final List<Listing> listings = new ArrayList<>();
    private Config cfg;
    private final MailManager mailManager;
    private final EconomyManager economyManager;
    private static final long SIX_HOURS_MS = 6 * 60 * 60 * 1000L;

    public MarketManager(ZombieLooterX plugin, MailManager mailManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.mailManager = mailManager;
        this.economyManager = economyManager;
        load();
        startMaintenanceTask();
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
                String typeStr = String.valueOf(m.getOrDefault("type", "SELL"));
                Type type = typeStr.equalsIgnoreCase("BUY") ? Type.BUY : Type.SELL;
                boolean fake = Boolean.parseBoolean(String.valueOf(m.getOrDefault("fake", false)));
                long expiresAt = ((Number) m.getOrDefault("expiresAt", System.currentTimeMillis() + SIX_HOURS_MS)).longValue();
                int reserved = ((Number) m.getOrDefault("reserved", 0)).intValue();
                listings.add(new Listing(seller, itemId, amount, price, type, fake, expiresAt, reserved));
            } catch (Exception ignored) {}
        }
        cleanupExpired();
        seedFakeListings();
    }

    public void save() {
        List<Map<String,Object>> arr = new ArrayList<>();
        for (Listing l : listings) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("seller", l.seller.toString());
            m.put("item", l.itemId);
            m.put("amount", l.amount);
            m.put("price", l.price);
            m.put("type", l.type.name());
            m.put("fake", l.fake);
            m.put("expiresAt", l.expiresAt);
            m.put("reserved", l.reserved);
            arr.add(m);
        }
        cfg.set("listings", arr); cfg.save();
    }

    public List<Listing> getListings(){ return Collections.unmodifiableList(listings); }

    public void list(Player p, String itemId, int amount, int price) {
        listings.add(new Listing(p.getUniqueId(), itemId, amount, price, Type.SELL, false, System.currentTimeMillis() + SIX_HOURS_MS, 0));
        save();
    }

    public boolean listBuy(Player p, String itemId, int amount, int price) {
        int total = price; // price already represents the bundle
        if (!economyManager.withdraw(p.getUniqueId(), total)) {
            return false;
        }
        listings.add(new Listing(p.getUniqueId(), itemId, amount, price, Type.BUY, false, System.currentTimeMillis() + SIX_HOURS_MS, total));
        save();
        return true;
    }

    public boolean buy(Player buyer, int index) {
        if (index < 0 || index >= listings.size()) return false;
        Listing l = listings.get(index);
        if (l.expiresAt < System.currentTimeMillis()) {
            listings.remove(index);
            save();
            return false;
        }
        if (l.type == Type.BUY) return false; // wrong method
        if (!plugin.getEconomyManager().withdraw(buyer.getUniqueId(), l.price)) return false;
        if (!l.fake) {
            plugin.getEconomyManager().addBalance(l.seller, l.price);
        }
        Item item = Item.get(itemIdToKey(l.itemId));
        if (item == null) return false;
        item.setCount(l.amount);
        buyer.getInventory().addItem(item);
        listings.remove(index);
        save();
        return true;
    }

    public boolean sellToBuyListing(Player seller, int index) {
        if (index < 0 || index >= listings.size()) return false;
        Listing l = listings.get(index);
        if (l.type != Type.BUY) return false;
        if (l.expiresAt < System.currentTimeMillis()) {
            listings.remove(index);
            save();
            return false;
        }

        Item item = Item.get(itemIdToKey(l.itemId));
        if (item == null) return false;
        item.setCount(l.amount);

        if (!seller.getInventory().contains(item)) {
            return false;
        }

        // Remove items from seller
        seller.getInventory().removeItem(item);

        // Pay seller from reserved funds or from the system if fake
        if (l.fake) {
            economyManager.addBalance(seller.getUniqueId(), l.price);
        } else {
            economyManager.addBalance(seller.getUniqueId(), l.price);
            l.reserved = Math.max(0, l.reserved - l.price);
        }

        // Deliver item to requester via mail (offline safe)
        if (!l.fake) {
            mailManager.addMail(l.seller, l.itemId, l.amount);
            Player target = plugin.getServer().getPlayer(l.seller);
            if (target != null) {
                target.sendMessage("§aYour buy order was fulfilled. Check /mail.");
            }
        }

        listings.remove(index);
        save();
        return true;
    }

    private String itemIdToKey(String s) { return s; } // placeholder mapping if needed

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        Iterator<Listing> it = listings.iterator();
        while (it.hasNext()) {
            Listing l = it.next();
            if (l.expiresAt < now) {
                if (!l.fake && l.type == Type.BUY && l.reserved > 0) {
                    economyManager.addBalance(l.seller, l.reserved);
                }
                it.remove();
            }
        }
        save();
    }

    private void seedFakeListings() {
        int target = 6;
        int currentFake = 0;
        for (Listing l : listings) {
            if (l.fake) currentFake++;
        }
        if (currentFake >= target) return;
        UUID system = new UUID(0L, 0L);
        String[][] templates = new String[][]{
                {"SELL","minecraft:bread","6","45"},
                {"SELL","minecraft:iron_sword","1","200"},
                {"BUY","minecraft:rotten_flesh","16","35"},
                {"BUY","minecraft:prismarine_shard","2","250"},
                {"SELL","minecraft:iron_ingot","8","90"},
                {"SELL","minecraft:leather","10","70"}
        };
        for (String[] t : templates) {
            if (currentFake >= target) break;
            Type type = t[0].equalsIgnoreCase("BUY") ? Type.BUY : Type.SELL;
            listings.add(new Listing(system, t[1], Integer.parseInt(t[2]), Integer.parseInt(t[3]), type, true, System.currentTimeMillis() + SIX_HOURS_MS, 0));
            currentFake++;
        }
        save();
    }

    private void startMaintenanceTask() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new cn.nukkit.scheduler.Task() {
            @Override
            public void onRun(int currentTick) {
                cleanupExpired();
                seedFakeListings();
            }
        }, 20 * 60 * 10); // every 10 minutes
    }
}
