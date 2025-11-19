package com.zombielooter.mail;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.*;

/**
 * Lightweight offline mail storage. Items stay persisted until claimed.
 * When inbox is full, new mail is queued in overflow and surfaced once space opens.
 */
public class MailManager {

    public static class MailEntry {
        public final String itemId;
        public int amount;
        public final long createdAt;

        public MailEntry(String itemId, int amount, long createdAt) {
            this.itemId = itemId;
            this.amount = amount;
            this.createdAt = createdAt;
        }
    }

    private static class Mailbox {
        final List<MailEntry> inbox = new ArrayList<>();
        final Deque<MailEntry> overflow = new ArrayDeque<>();
    }

    private final ZombieLooterX plugin;
    private final Map<UUID, Mailbox> mailboxes = new HashMap<>();
    private final Config storage;
    private static final int INBOX_LIMIT = 27; // visible entries

    public MailManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "mail.yml");
        if (!file.exists()) {
            plugin.saveResource("mail.yml", false);
        }
        this.storage = new Config(file, Config.YAML);
        load();
    }

    private void load() {
        mailboxes.clear();
        Map<String, Object> raw = storage.getAll();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            try {
                UUID uuid = UUID.fromString(entry.getKey());
                Mailbox box = new Mailbox();
                Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                List<Map<String, Object>> inboxList = (List<Map<String, Object>>) playerData.getOrDefault("inbox", new ArrayList<>());
                for (Map<String, Object> m : inboxList) {
                    box.inbox.add(mapToEntry(m));
                }
                List<Map<String, Object>> overflowList = (List<Map<String, Object>>) playerData.getOrDefault("overflow", new ArrayList<>());
                for (Map<String, Object> m : overflowList) {
                    box.overflow.addLast(mapToEntry(m));
                }
                mailboxes.put(uuid, box);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Mailbox> entry : mailboxes.entrySet()) {
            Map<String, Object> playerData = new LinkedHashMap<>();
            List<Map<String, Object>> inbox = new ArrayList<>();
            for (MailEntry me : entry.getValue().inbox) {
                inbox.add(entryToMap(me));
            }
            List<Map<String, Object>> overflow = new ArrayList<>();
            for (MailEntry me : entry.getValue().overflow) {
                overflow.add(entryToMap(me));
            }
            playerData.put("inbox", inbox);
            playerData.put("overflow", overflow);
            out.put(entry.getKey().toString(), playerData);
        }
        storage.setAll(out);
        storage.save();
    }

    public List<MailEntry> viewInbox(UUID playerId) {
        return Collections.unmodifiableList(getMailbox(playerId).inbox);
    }

    public int getOverflowCount(UUID playerId) {
        return getMailbox(playerId).overflow.size();
    }

    public void addMail(UUID recipient, String itemId, int amount) {
        Mailbox box = getMailbox(recipient);
        MailEntry entry = new MailEntry(itemId, amount, System.currentTimeMillis());
        if (box.inbox.size() < INBOX_LIMIT) {
            box.inbox.add(entry);
        } else {
            box.overflow.addLast(entry);
            Optional<Player> p = plugin.getServer().getPlayer(recipient);
            p.ifPresent(player -> player.sendMessage("§eYour mail is full. New items will stay queued until you claim some."));
        }
        save();
    }

    public void claimAll(Player player) {
        Mailbox box = getMailbox(player.getUniqueId());
        Iterator<MailEntry> it = box.inbox.iterator();
        int claimed = 0;
        while (it.hasNext()) {
            MailEntry entry = it.next();
            if (deliver(player, entry)) {
                claimed++;
                it.remove();
            } else {
                player.sendMessage("§cInventory full. Claim space before taking more mail.");
                break;
            }
        }
        if (claimed > 0) {
            refillFromOverflow(box);
        }
        save();
    }

    public void claimSingle(Player player, int index) {
        Mailbox box = getMailbox(player.getUniqueId());
        if (index < 0 || index >= box.inbox.size()) {
            player.sendMessage("§cInvalid mail index.");
            return;
        }
        MailEntry entry = box.inbox.get(index);
        if (deliver(player, entry)) {
            box.inbox.remove(index);
            refillFromOverflow(box);
            save();
            player.sendMessage("§aClaimed mail #" + index + ".");
        } else {
            player.sendMessage("§cInventory full. Free up space and try again.");
        }
    }

    private boolean deliver(Player player, MailEntry entry) {
        Item item = Item.get(entry.itemId);
        if (item == null) {
            return true; // skip invalid items
        }
        item.setCount(entry.amount);
        Item[] leftover = player.getInventory().addItem(item);
        return leftover.length == 0;
    }

    private void refillFromOverflow(Mailbox box) {
        while (box.inbox.size() < INBOX_LIMIT && !box.overflow.isEmpty()) {
            box.inbox.add(box.overflow.pollFirst());
        }
    }

    private Mailbox getMailbox(UUID id) {
        return mailboxes.computeIfAbsent(id, k -> new Mailbox());
    }

    private MailEntry mapToEntry(Map<String, Object> m) {
        String itemId = String.valueOf(m.get("item"));
        int amount = ((Number) m.getOrDefault("amount", 1)).intValue();
        long created = ((Number) m.getOrDefault("created", System.currentTimeMillis())).longValue();
        return new MailEntry(itemId, amount, created);
    }

    private Map<String, Object> entryToMap(MailEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("item", e.itemId);
        m.put("amount", e.amount);
        m.put("created", e.createdAt);
        return m;
    }
}
