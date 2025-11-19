package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.inventory.*;
import cn.nukkit.inventory.fake.FakeInventory;
import cn.nukkit.inventory.fake.FakeInventoryType;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemDarkOakBoat;
import cn.nukkit.item.ItemID;
import cn.nukkit.plugin.InternalPlugin;
import com.zombielooter.LootManager;
import com.zombielooter.ZombieLooterX;

import java.util.List;
import java.util.Map;

public class LootPreviewCommand implements CommandExecutor {

    private final ZombieLooterX plugin;
    private final LootManager lootManager;

    public LootPreviewCommand(ZombieLooterX plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;
        FakeInventory inv = new FakeInventory(FakeInventoryType.ENDER_CHEST, "Loot");

        inv.setDefaultItemHandler((i, slot, item, item2, event)->{
            if(item != null) event.setCancelled();
        });
        inv.setContents(lootManager.getAllPossibleLootItems());

        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> player.addWindow(inv), 20);

        return true;
    }
}
