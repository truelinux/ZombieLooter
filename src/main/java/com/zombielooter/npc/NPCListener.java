package com.zombielooter.npc;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.Vendor;
import com.zombielooter.gui.VendorMenuUI;

public class NPCListener implements Listener {

    private final ZombieLooterX plugin;
    private final NPCManager npcManager;

    public NPCListener(ZombieLooterX plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        // Correct way to check for a right-click interaction in PowerNukkitX
        if (event.getEntity() instanceof VendorNPC) {
            // Directly get the vendor info from the entity itself
            Vendor vendor = npcManager.getVendorFromEntity(event.getEntity());
            if (vendor != null) {
                // This is a vendor NPC, open the menu for this specific vendor
                VendorMenuUI.openMainMenu(plugin, player, vendor.getId());
                event.setCancelled();
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        // Correct way to check for a right-click interaction in PowerNukkitX
        if (event.getEntity() instanceof VendorNPC) {
            event.setCancelled(true);
        }
    }
}
