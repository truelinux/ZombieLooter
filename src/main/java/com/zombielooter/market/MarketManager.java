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
        public final int meta;
        public final String displayName;
        public final String itemData;
        public final int amount;
        public final int price;
        public final Type type;
        public final boolean fake;
        public final long expiresAt;
        public int reserved;
        public final String vendorName;

        public Listing(UUID seller, String itemId, int meta, int amount, int price, Type type, boolean fake, long expiresAt, int reserved, String vendorName, String displayName, String itemData) {
            this.seller = seller;
            this.itemId = itemId;
            this.meta = meta;
            this.displayName = displayName;
            this.itemData = itemData;
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
    private int maxFakeListings = 6;
    private Config cfg;
    private final MailManager mailManager;
    private final EconomyManager economyManager;
    private final GUITextManager text;
    private static final long SIX_HOURS_MS = 6 * 60 * 60 * 1000L;
    private static final int PAGE_SIZE = 45; // leave bottom row for navigation
    private static final UUID SYSTEM_SELLER = new UUID(0L, 0L);
    private static final Random RANDOM = new Random();
    private static final String[] FAKE_VENDOR_NAMES = {
            "Redwood Traders", "Iron Syndicate", "Dusty Caravan", "Night Bazaar",
            "Gutter Exchange", "Frontier Broker", "Silent Courier", "Ashen Merchants",
            "Haven Market", "Echo Supply"
    };
    private static final List<FakeListingTemplate> FAKE_LISTING_TEMPLATES = Arrays.asList(
            new FakeListingTemplate(Type.SELL, "minecraft:bread", 0, 4, 16, 8),
            new FakeListingTemplate(Type.SELL, "minecraft:iron_ingot", 0, 3, 12, 14),
            new FakeListingTemplate(Type.SELL, "minecraft:leather", 0, 4, 16, 7),
            new FakeListingTemplate(Type.SELL, "minecraft:arrow", 0, 16, 64, 2),
            new FakeListingTemplate(Type.SELL, "minecraft:iron_sword", 0, 1, 1, 220),
            new FakeListingTemplate(Type.SELL, "minecraft:golden_apple", 0, 1, 3, 150),
            new FakeListingTemplate(Type.SELL, "minecraft:gunpowder", 0, 8, 32, 5),
            new FakeListingTemplate(Type.BUY, "minecraft:rotten_flesh", 0, 12, 32, 3),
            new FakeListingTemplate(Type.BUY, "minecraft:spider_eye", 0, 6, 18, 6),
            new FakeListingTemplate(Type.BUY, "minecraft:prismarine_shard", 0, 2, 8, 35)
    );

    private static class FakeListingTemplate {
        final Type type;
        final String itemId;
        final int meta;
        final int minAmount;
        final int maxAmount;
        final int basePricePerItem;

        FakeListingTemplate(Type type, String itemId, int meta, int minAmount, int maxAmount, int basePricePerItem) {
            this.type = type;
            this.itemId = itemId;
            this.meta = meta;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.basePricePerItem = basePricePerItem;
        }
    }

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
        this.maxFakeListings = Math.max(0, cfg.getInt("fake_listings.max", 6));
        listings.clear();
        List<Map<String,Object>> arr = (List<Map<String,Object>>) cfg.getList("listings", new ArrayList<>());
        for (Map<String,Object> m : arr) {
            try {
                UUID seller = java.util.UUID.fromString(String.valueOf(m.get("seller")));
                String itemId = String.valueOf(m.get("item"));
                int meta = ((Number) m.getOrDefault("meta", 0)).intValue();
                int amount = ((Number) m.getOrDefault("amount", 1)).intValue();
                int price = ((Number) m.getOrDefault("price", 1)).intValue();
                String typeStr = String.valueOf(m.getOrDefault("type", "SELL"));
                Type type = typeStr.equalsIgnoreCase("BUY") ? Type.BUY : Type.SELL;
                boolean fake = Boolean.parseBoolean(String.valueOf(m.getOrDefault("fake", false)));
                long expiresAt = ((Number) m.getOrDefault("expiresAt", System.currentTimeMillis() + SIX_HOURS_MS)).longValue();
                int reserved = ((Number) m.getOrDefault("reserved", 0)).intValue();
                String vendorName = String.valueOf(m.getOrDefault("vendor", ""));
                Object rawName = m.get("name");
                String storedName = rawName == null ? "" : rawName.toString();
                String itemData = String.valueOf(m.getOrDefault("nbt", ""));
                String displayName = determineDisplayName(null, itemId, storedName);
                listings.add(new Listing(seller, itemId, meta, amount, price, type, fake, expiresAt, reserved, vendorName, displayName, itemData));
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
            m.put("meta", l.meta);
            m.put("amount", l.amount);
            m.put("price", l.price);
            m.put("type", l.type.name());
            m.put("fake", l.fake);
            m.put("expiresAt", l.expiresAt);
            m.put("reserved", l.reserved);
            m.put("vendor", l.vendorName == null ? "" : l.vendorName);
            m.put("name", l.displayName == null ? "" : l.displayName);
            m.put("nbt", l.itemData == null ? "" : l.itemData);
            arr.add(m);
        }
        cfg.set("listings", arr); cfg.save();
    }

    public List<Listing> getListings(){ return Collections.unmodifiableList(listings); }

    public boolean list(Player player, String itemId, int amount, int price) {
        if (amount <= 0 || price <= 0) return false;
        Item base = Item.get(itemIdToKey(itemId));
        if (isAir(base)) return false;
        Item extracted = removeFromInventory(player, base, amount);
        if (extracted == null) return false;
        createSellListing(player, extracted, price);
        return true;
    }

    public boolean listFromHand(Player player, int amount, int price) {
        if (amount <= 0 || price <= 0) return false;
        Item hand = player.getInventory().getItemInHand();
        if (isAir(hand) || hand.getCount() < amount) return false;
        Item extracted = hand.clone();
        extracted.setCount(amount);
        hand.setCount(hand.getCount() - amount);
        if (hand.getCount() <= 0) {
            player.getInventory().setItemInHand(Item.AIR.clone());
        } else {
            player.getInventory().setItemInHand(hand);
        }
        createSellListing(player, extracted, price);
        return true;
    }

    public boolean listBuy(Player player, String itemId, int amount, int price) {
        Item template = Item.get(itemIdToKey(itemId));
        if (isAir(template)) return false;
        return createBuyListing(player, itemId, template.getDamage(), "", amount, price, template);
    }

    public boolean listBuyFromHand(Player player, int amount, int price) {
        if (amount <= 0 || price <= 0) return false;
        Item hand = player.getInventory().getItemInHand();
        if (isAir(hand)) return false;
        String encoded = encodeItemData(hand);
        String key = itemKeyFromItem(hand);
        return createBuyListing(player, key, hand.getDamage(), encoded, amount, price, hand);
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
        Item item = buildItemFromListing(l);
        if (item == null) return false;
        Item[] leftover = buyer.getInventory().addItem(item);
        if (leftover.length > 0) {
            for (Item extra : leftover) {
                mailManager.addMail(buyer.getUniqueId(), extra);
            }
            buyer.sendMessage(TextFormat.colorize('&', text.get("commands.market.mail_overflow", "&eInventory full. Extra items sent to /mail.")));
        }
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

        Item template = Item.get(itemIdToKey(l.itemId));
        if (isAir(template)) return false;
        template.setDamage(l.meta);

        List<Item> deliveredStacks = takeItemsMatching(seller, template, l.amount);
        if (deliveredStacks == null) {
            seller.sendMessage(TextFormat.colorize('&', text.get("commands.market.not_enough_items", "&cYou do not have the required stack with that data.")));
            return false;
        }

        // Pay seller from reserved funds or from the system if fake
        if (l.fake) {
            economyManager.addBalance(seller.getUniqueId(), l.price);
        } else {
            economyManager.addBalance(seller.getUniqueId(), l.price);
            l.reserved = Math.max(0, l.reserved - l.price);
        }

        // Deliver item to requester via mail (offline safe)
        if (!l.fake) {
            for (Item chunk : deliveredStacks) {
                mailManager.addMail(l.seller, chunk);
            }
            Optional<Player> target = plugin.getServer().getPlayer(l.seller);
            target.ifPresent(player -> player.sendMessage(TextFormat.colorize('&', "&aYour buy order was fulfilled. Check /mail.")));
        }

        listings.remove(index);
        save();
        return true;
    }

    private void createSellListing(Player owner, Item stack, int price) {
        String key = itemKeyFromItem(stack);
        String displayName = getDisplayNameFromItem(owner, stack, key);
        String encoded = encodeItemData(stack);
        listings.add(new Listing(owner.getUniqueId(), key, stack.getDamage(), stack.getCount(), price, Type.SELL, false, System.currentTimeMillis() + SIX_HOURS_MS, 0, owner.getName(), displayName, encoded));
        save();
    }

    private boolean createBuyListing(Player owner, String itemKey, int meta, String encodedData, int amount, int price, Item template) {
        if (amount <= 0 || price <= 0) return false;
        int total = price;
        if (!economyManager.withdraw(owner.getUniqueId(), total)) {
            return false;
        }
        String displayName = getDisplayNameFromItem(owner, template, itemKey);
        listings.add(new Listing(owner.getUniqueId(), itemKey, meta, amount, price, Type.BUY, false, System.currentTimeMillis() + SIX_HOURS_MS, total, owner.getName(), displayName, encodedData == null ? "" : encodedData));
        save();
        return true;
    }

    private Item buildItemFromListing(Listing listing) {
        byte[] data = decodeItemData(listing.itemData);
        Item item;
        if (data != null && data.length > 0) {
            item = Item.get(itemIdToKey(listing.itemId), listing.meta, listing.amount, data);
        } else {
            item = Item.get(itemIdToKey(listing.itemId), listing.meta, listing.amount);
        }
        return item;
    }

    private Item removeFromInventory(Player player, Item target, int amount) {
        if (player == null || player.getInventory() == null) return null;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item stack = player.getInventory().getItem(slot);
            if (!matches(stack, target)) continue;
            if (stack.getCount() < amount) continue;
            Item extracted = stack.clone();
            extracted.setCount(amount);
            stack.setCount(stack.getCount() - amount);
            player.getInventory().setItem(slot, stack.getCount() <= 0 ? Item.AIR.clone() : stack);
            return extracted;
        }
        return null;
    }

    private List<Item> takeItemsMatching(Player player, Item target, int amount) {
        if (player == null || player.getInventory() == null) return null;
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            Item stack = player.getInventory().getItem(slot);
            if (matches(stack, target)) {
                total += stack.getCount();
            }
        }
        if (total < amount) {
            return null;
        }
        List<Item> taken = new ArrayList<>();
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
            Item stack = player.getInventory().getItem(slot);
            if (!matches(stack, target)) continue;
            int take = Math.min(remaining, stack.getCount());
            if (take <= 0) continue;
            Item chunk = stack.clone();
            chunk.setCount(take);
            taken.add(chunk);
            stack.setCount(stack.getCount() - take);
            player.getInventory().setItem(slot, stack.getCount() <= 0 ? Item.AIR.clone() : stack);
            remaining -= take;
        }
        return taken;
    }

    private boolean matches(Item stack, Item target) {
        if (isAir(stack) || target == null) return false;
        return stack.getId() == target.getId() && stack.getDamage() == target.getDamage();
    }

    private boolean isAir(Item item) {
        return item == null || item.getId() == Item.AIR.getId();
    }

    private String itemKeyFromItem(Item item) {
        if (item == null) return "minecraft:air";
        if (item.getIdentifier() != null) {
            return item.getIdentifier().toString();
        }
        return String.valueOf(item.getId());
    }

    private String getDisplayNameFromItem(Player owner, Item stack, String fallbackId) {
        if (stack != null) {
            if (stack.hasCustomName()) return stack.getCustomName();
            String stackName = stack.getName();
            if (stackName != null && !stackName.isEmpty()) {
                return stackName;
            }
        }
        return determineDisplayName(owner, fallbackId, "");
    }

    private String encodeItemData(Item item) {
        if (item == null) return "";
        CompoundTag tag = item.getNamedTag();
        if (tag == null) return "";
        try {
            byte[] data = item.writeCompoundTag(tag);
            if (data == null || data.length == 0) return "";
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception ignored) {
            return "";
        }
    }

    private byte[] decodeItemData(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String itemIdToKey(String s) { return s; } // placeholder mapping if needed

    private String getListingDisplayName(Listing listing) {
        String stored = listing.displayName == null ? "" : listing.displayName;
        return determineDisplayName(null, listing.itemId, stored);
    }

    private String determineDisplayName(Player owner, String itemId, String storedName) {
        if (storedName != null) {
            String trimmed = storedName.trim();
            if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed)) {
                return storedName;
            }
        }
        Item base = Item.get(itemIdToKey(itemId));
        if (base != null) {
            if (owner != null && owner.getInventory() != null) {
                Item hand = owner.getInventory().getItemInHand();
                if (hand != null && hand.getId() == base.getId() && hand.getDamage() == base.getDamage() && hand.hasCustomName()) {
                    return hand.getCustomName();
                }
                for (Item invItem : owner.getInventory().getContents().values()) {
                    if (invItem == null) continue;
                    if (invItem.getId() == base.getId() && invItem.getDamage() == base.getDamage() && invItem.hasCustomName()) {
                        return invItem.getCustomName();
                    }
                }
            }
            if (base.hasCustomName()) {
                return base.getCustomName();
            }
            String defaultName = base.getName();
            if (defaultName != null && !defaultName.isEmpty()) {
                return defaultName;
            }
        }
        return itemId;
    }

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
        if (maxFakeListings <= 0) return;
        int missing = Math.max(0, maxFakeListings - countFakeListings());
        if (missing <= 0) return;
        boolean changed = false;
        for (int i = 0; i < missing; i++) {
            Listing listing = createRandomFakeListing();
            if (listing == null) {
                break;
            }
            listings.add(listing);
            changed = true;
        }
        if (changed) save();
    }

    private void addHourlyFakeListingIfNeeded() {
        if (maxFakeListings <= 0) return;
        if (countFakeListings() >= maxFakeListings) return;
        Listing listing = createRandomFakeListing();
        if (listing == null) return;
        listings.add(listing);
        save();
    }

    private int countFakeListings() {
        int currentFake = 0;
        for (Listing l : listings) {
            if (l.fake) currentFake++;
        }
        return currentFake;
    }

    private Listing createRandomFakeListing() {
        if (FAKE_LISTING_TEMPLATES.isEmpty()) return null;
        FakeListingTemplate template = FAKE_LISTING_TEMPLATES.get(RANDOM.nextInt(FAKE_LISTING_TEMPLATES.size()));
        int amountRange = template.maxAmount - template.minAmount;
        int amount = template.minAmount + (amountRange <= 0 ? 0 : RANDOM.nextInt(amountRange + 1));
        double multiplier = priceBias(template.type);
        int totalPrice = Math.max(1, (int) Math.round(amount * template.basePricePerItem * multiplier));
        String vendor = FAKE_VENDOR_NAMES[RANDOM.nextInt(FAKE_VENDOR_NAMES.length)];
        String displayName = determineDisplayName(null, template.itemId, "");
        return new Listing(SYSTEM_SELLER, template.itemId, template.meta, amount, totalPrice, template.type, true, System.currentTimeMillis() + SIX_HOURS_MS, 0, vendor, displayName, "");
    }

    private double priceBias(Type type) {
        if (type == Type.SELL) {
            double roll = RANDOM.nextDouble();
            if (roll < 0.15) {
                return randomRange(0.65, 0.9); // rare deal
            } else if (roll < 0.6) {
                return randomRange(0.9, 1.2);
            }
            return randomRange(1.2, 1.75);
        } else {
            double roll = RANDOM.nextDouble();
            if (roll < 0.2) {
                return randomRange(1.15, 1.4); // rare generous price
            } else if (roll < 0.65) {
                return randomRange(0.85, 1.1);
            }
            return randomRange(0.55, 0.85);
        }
    }

    private double randomRange(double min, double max) {
        if (max <= min) return min;
        return min + (max - min) * RANDOM.nextDouble();
    }

    private void startMaintenanceTask() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new cn.nukkit.scheduler.Task() {
            @Override
            public void onRun(int currentTick) {
                cleanupExpired();
            }
        }, 20 * 60 * 10); // every 10 minutes

        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new cn.nukkit.scheduler.Task() {
            @Override
            public void onRun(int currentTick) {
                addHourlyFakeListingIfNeeded();
            }
        }, 20 * 60 * 60); // every hour
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
            Item display = buildItemFromListing(l);
            if (display == null) display = Item.get("minecraft:paper");
            else display = display.clone();
            display.setCount(Math.min(l.amount, display.getMaxStackSize()));

            String sellerName = l.fake ? getVendorName(l) : resolveName(l.seller);
            long minutesLeft = Math.max(0, (l.expiresAt - now) / 60000);
            String typeLabel = l.type == Type.BUY ? text.get("commands.market.type_buy", "&aBUY") : text.get("commands.market.type_sell", "&bSELL");
            String listingName = getListingDisplayName(l);
            display.setCustomName(TextFormat.colorize('&', typeLabel + " &f" + l.amount + "x " + listingName));
            List<String> lore = new ArrayList<>();
            lore.add(String.format(text.get("commands.market.lore_item", "&7Item: &f%s"), listingName)); //TODO: Remove this line as the listing name is already dispalyed
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





