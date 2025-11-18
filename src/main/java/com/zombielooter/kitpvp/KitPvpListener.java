package com.zombielooter.kitpvp;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;

public class KitPvpListener implements Listener {

    private final KitPvpManager manager;

    public KitPvpListener(KitPvpManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        manager.markActivity(player);
        KitChest chest = manager.findChest(event.getBlock());
        if (chest == null) return;
        event.setCancelled();
        KitDefinition kit = manager.getKit(chest.getKitId());
        if (kit == null) {
            player.sendMessage(TextFormat.colorize('&', "&cKit not found."));
            return;
        }
        manager.openPreview(player, kit);
    }

    @EventHandler
    public void onInventoryTransaction(InventoryTransactionEvent event) {
        Player player = event.getSource();
        if (!manager.isPreviewing(player)) return;
        boolean containsFake = event.getTransaction().getInventories().stream().anyMatch(inv -> inv instanceof cn.nukkit.inventory.fake.FakeInventory);
        if (!containsFake) return;
        event.setCancelled();
        player.removeAllWindows();
        manager.applyPreviewSelection(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!manager.isKitWorld(victim)) return;

        event.setKeepInventory(true);
        event.setDrops(new Item[0]);
        manager.triggerDeathEffects(victim);
        manager.handleDeath(victim);

        if (victim.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) victim.getLastDamageCause();
            if (dmg.getDamager() instanceof Player) {
                manager.handleKill((Player) dmg.getDamager(), victim);
            }
        }

        manager.sendToSelection(victim);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (manager.isKitWorld(player)) {
            manager.markActivity(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager.isParticipant(player)) {
            manager.leave(player);
        }
    }
}
