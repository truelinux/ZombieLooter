package com.zombielooter.market;

import cn.nukkit.Player;
import cn.nukkit.inventory.fake.FakeInventory;
import cn.nukkit.inventory.fake.FakeInventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.EconomyManager;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.mail.MailManager;

import java.io.File;
import java.util.*;
import cn.nukkit.utils.TextFormat;

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
        public final String vendorName;

        public Listing(UUID seller, String itemId, int amount, int price, Type type, boolean fake, long expiresAt, int reserved, String vendorName) {
            this.seller = seller;
            this.itemId = itemId;
            this.amount = amount;
            this.price = price;
            this.type = type;
            this.fake = fake;
            this.expiresAt = expiresAt;
            this.reserved = reserved;
            this.vendorName = vendorName;
        }
    }

    public enum Type { SELL, BUY }

    private final ZombieLooterX plugin;
    private final List<Listing> listings = new ArrayList<>();
    private Config cfg;
    private final MailManager mailManager;
    private final EconomyManager economyManager;
    private final GUITextManager text;
    private static final long SIX_HOURS_MS = 6 * 60 * 60 * 1000L;
    private static final int PAGE_SIZE = 45; // leave bottom row for navigation
    private static final String[] FAKE_VENDOR_NAMES = {
            "Redwood Traders", "Iron Syndicate", "Dusty Caravan", "Night Bazaar",
            "Gutter Exchange", "Frontier Broker", "Silent Courier", "Ashen Merchants",
            "Haven Market", "Echo Supply"
    };

    public MarketManager(ZombieLooterX plugin, MailManager mailManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.mailManager = mailManager;
        this.economyManager = economyManager;
        this.text = plugin.getGUITextManager();
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
                String vendorName = String.valueOf(m.getOrDefault("vendor", ""));
                listings.add(new Listing(seller, itemId, amount, price, type, fake, expiresAt, reserved, vendorName));
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
            m.put("vendor", l.vendorName == null ? "" : l.vendorName);
            arr.add(m);
        }
        cfg.set("listings", arr); cfg.save();
    }

    public List<Listing> getListings(){ return Collections.unmodifiableList(listings); }

    public void list(Player p, String itemId, int amount, int price) {
        listings.add(new Listing(p.getUniqueId(), itemId, amount, price, Type.SELL, false, System.currentTimeMillis() + SIX_HOURS_MS, 0, p.getName()));
        save();
    }

    public boolean listBuy(Player p, String itemId, int amount, int price) {
        int total = price; // price already represents the bundle
        if (!economyManager.withdraw(p.getUniqueId(), total)) {
            return false;
        }
        listings.add(new Listing(p.getUniqueId(), itemId, amount, price, Type.BUY, false, System.currentTimeMillis() + SIX_HOURS_MS, total, p.getName()));
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
            Optional<Player> target = plugin.getServer().getPlayer(l.seller);
            target.ifPresent(player -> player.sendMessage(TextFormat.colorize('&', "&aYour buy order was fulfilled. Check /mail.")));
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
            String vendor = FAKE_VENDOR_NAMES[new Random().nextInt(FAKE_VENDOR_NAMES.length)];
            listings.add(new Listing(system, t[1], Integer.parseInt(t[2]), Integer.parseInt(t[3]), type, true, System.currentTimeMillis() + SIX_HOURS_MS, 0, vendor));
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

    public void openListingInventory(Player player, int page) {
        List<Listing> snapshot = new ArrayList<>(listings);
        int totalPages = Math.max(1, (int) Math.ceil(snapshot.size() / (double) PAGE_SIZE));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        String titleRaw = text.get("commands.market.inv_title", "&lMarket %d/%d");
        String title = String.format(titleRaw, safePage + 1, totalPages);
        FakeInventory inv = new FakeInventory(FakeInventoryType.DOUBLE_CHEST, title);

        // Cancel all take/put and handle clicks
        inv.setDefaultItemHandler((inventory, slot, oldItem, newItem, event) -> {
            event.setCancelled();
            if (oldItem == null || !oldItem.hasCompoundTag()) return;
            CompoundTag tag = oldItem.getNamedTag();
            if (tag.contains("navTarget")) {
                int target = tag.getInt("navTarget");
                plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> openListingInventory(player, target), 2);
                return;
            }
            if (!tag.contains("listingIndex")) return;
            int idx = tag.getInt("listingIndex");
            if (idx < 0 || idx >= listings.size()) {
                player.sendMessage(TextFormat.colorize('&', "&cThat listing expired."));
                openListingInventory(player, safePage);
                return;
            }
            Listing l = listings.get(idx);
            boolean success = l.type == Type.SELL ? buy(player, idx) : sellToBuyListing(player, idx);
            if (!success) {
                player.sendMessage(TextFormat.colorize('&', "&cCould not complete that listing."));
            }
            plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> openListingInventory(player, safePage), 2);
        });

        int start = safePage * PAGE_SIZE;
        int placed = 0;
        long now = System.currentTimeMillis();
        for (int i = start; i < snapshot.size() && placed < PAGE_SIZE; i++) {
            Listing l = snapshot.get(i);
            Item display = Item.get(itemIdToKey(l.itemId));
            if (display == null) display = Item.get("minecraft:paper");
            display.setCount(Math.min(l.amount, display.getMaxStackSize()));

            String sellerName = l.fake ? getVendorName(l) : resolveName(l.seller);
            long minutesLeft = Math.max(0, (l.expiresAt - now) / 60000);
            String typeLabel = l.type == Type.BUY ? text.get("commands.market.type_buy", "&aBUY") : text.get("commands.market.type_sell", "&bSELL");
            display.setCustomName(TextFormat.colorize('&', typeLabel + " &f" + l.amount + "x " + l.itemId));
            List<String> lore = new ArrayList<>();
            lore.add(String.format(text.get("commands.market.lore_price", "&7Price: &6%s"), l.price));
            lore.add(String.format(text.get("commands.market.lore_by", "&7By: &f%s"), sellerName));
            lore.add(String.format(text.get("commands.market.lore_time", "&7Time left: &e%sm"), minutesLeft));
            lore.add(l.type == Type.SELL
                    ? text.get("commands.market.lore_action_buy", "&aClick to buy")
                    : text.get("commands.market.lore_action_sell", "&aClick to sell into this order"));
            display.setLore(lore.toArray(new String[0]));
            CompoundTag tag = display.hasCompoundTag() ? display.getNamedTag() : new CompoundTag();
            tag.putInt("listingIndex", i);
            display.setNamedTag(tag);
            inv.setItem(placed, display);
            placed++;
        }

        // Navigation controls
        if (safePage > 0) {
            Item prev = Item.get("minecraft:arrow");
            prev.setCustomName(text.get("commands.market.nav_prev", "&ePrevious Page"));
            CompoundTag t = new CompoundTag();
            t.putInt("navTarget", safePage - 1);
            prev.setNamedTag(t);
            inv.setItem(51, prev);
        }
        if (safePage < totalPages - 1) {
            Item next = Item.get("minecraft:arrow");
            next.setCustomName(text.get("commands.market.nav_next", "&eNext Page"));
            CompoundTag t = new CompoundTag();
            t.putInt("navTarget", safePage + 1);
            next.setNamedTag(t);
            inv.setItem(52, next);
        }

        // Quick switcher (always goes to the next page, wrapping to first)
        Item switcher = Item.get("minecraft:compass");
        switcher.setCustomName(text.get("commands.market.nav_switch", "&bSwitch Page"));
        List<String> sLore = new ArrayList<>();
        sLore.add(text.get("commands.market.nav_switch_lore", "&7Jump to next page without retyping."));
        switcher.setLore(sLore.toArray(new String[0]));
        CompoundTag sTag = new CompoundTag();
        int wrapNext = safePage + 1 >= totalPages ? 0 : safePage + 1;
        sTag.putInt("navTarget", wrapNext);
        switcher.setNamedTag(sTag);
        inv.setItem(53, switcher);

        // slight delay to ensure client opens inventory cleanly
        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> player.addWindow(inv), 20);
    }

    private String resolveName(UUID uuid) {
        Optional<Player> p = plugin.getServer().getPlayer(uuid);
        if (p.isPresent()) return p.get().getName();
        String u = uuid.toString();
        return "Player-" + u.substring(0, 5);
    }

    private String getVendorName(Listing l) {
        if (l.vendorName != null && !l.vendorName.isEmpty()) return l.vendorName;
        return FAKE_VENDOR_NAMES[new Random().nextInt(FAKE_VENDOR_NAMES.length)];
    }

}





