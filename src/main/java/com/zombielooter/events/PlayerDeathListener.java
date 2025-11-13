package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.item.Item;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.factions.Faction;
import com.zombielooter.factions.FactionManager;
import com.zombielooter.factions.PowerManager;
import com.zombielooter.factions.RaidManager;
import com.zombielooter.xp.XPManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerDeathListener implements Listener {

    private final ZombieLooterX plugin;
    private final XPManager xpManager;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final RaidManager raidManager;
    private final Random random = new Random();

    public PlayerDeathListener(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.xpManager = plugin.getXPManager();
        this.factionManager = plugin.getFactionManager();
        this.powerManager = plugin.getPowerManager();
        this.raidManager = plugin.getRaidManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        if (!(deceased.getLastDamageCause() instanceof EntityDamageByEntityEvent)) return;

        EntityDamageByEntityEvent lastDamage = (EntityDamageByEntityEvent) deceased.getLastDamageCause();
        if (!(lastDamage.getDamager() instanceof Player)) return;

        Player killer = (Player) lastDamage.getDamager();

        // --- Faction Power Logic ---
        Faction deceasedFaction = factionManager.getFactionByPlayer(deceased.getUniqueId());
        Faction killerFaction = factionManager.getFactionByPlayer(killer.getUniqueId());

        if (deceasedFaction != null) {
            powerManager.removePower(deceasedFaction, Math.abs(powerManager.getOnDeathLoss()));
            raidManager.updateFactionRaidableStatus(deceasedFaction);
            deceasedFaction.getMembers().forEach(memberUUID -> 
                plugin.getServer().getPlayer(memberUUID).ifPresent(member -> 
                    member.sendMessage("§cYour faction lost power because " + deceased.getName() + " died!")
                )
            );
        }

        if (killerFaction != null && !killerFaction.equals(deceasedFaction)) {
            powerManager.addPower(killerFaction, powerManager.getOnKillGain());
            raidManager.updateFactionRaidableStatus(killerFaction);
            killerFaction.getMembers().forEach(memberUUID -> 
                plugin.getServer().getPlayer(memberUUID).ifPresent(member -> 
                    member.sendMessage("§aYour faction gained power because " + killer.getName() + " killed an enemy!")
                )
            );
        }

        // --- Player Death Loot Logic ---
        int playerLevel = xpManager.getLevel(deceased.getUniqueId());
        List<Item> drops = calculateLoot(playerLevel);

        for (Item item : drops) {
            deceased.getLevel().dropItem(deceased.getLocation(), item);
        }
        event.setKeepInventory(true);
        event.setDrops(new Item[0]);
    }

    private List<Item> calculateLoot(int playerLevel) {
        List<Item> loot = new ArrayList<>();
        int numberOfItems = 1 + random.nextInt(3); // Drops 1 to 3 items

        for (int i = 0; i < numberOfItems; i++) {
            double rareChance = Math.min(0.75, playerLevel / 200.0);

            if (random.nextDouble() < rareChance) {
                loot.add(getRareItem());
            } else {
                loot.add(getCommonItem());
            }
        }
        return loot;
    }

    private Item getCommonItem() {
        int rand = random.nextInt(2);
        if (rand == 0) {
            return Item.get(Item.IRON_INGOT, 0, 1 + random.nextInt(3)); // 1-3 Scrap Metal
        } else {
            return Item.get("minecraft:iron_nugget", 0, 5 + random.nextInt(11)); // 5-15 Pistol Ammo
        }
    }

    private Item getRareItem() {
        int rand = random.nextInt(3);
        if (rand == 0) {
            return Item.get(Item.DIAMOND, 0, 1); // A valuable gem
        } else if (rand == 1) {
            return Item.get("minecraft:apple", 0, 1); // A Medkit
        } else {
            return Item.get("minecraft:netherite_ingot", 0, 1); // Military-Grade Alloy
        }
    }
}
