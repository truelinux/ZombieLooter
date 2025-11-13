package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.market.MarketManager;
import com.zombielooter.gui.MarketMenuUI;

public class MarketCommand implements CommandExecutor {
    private final ZombieLooterX plugin;
    private final GUITextManager textManager;

    public MarketCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.textManager = plugin.getGUITextManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(textManager.getText("commands.market.only_players", "§cPlayers only."));
            return true;
        }
        if (args.length == 0) {
            MarketMenuUI.openMainMenu(plugin, player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("list") && args.length >= 4) {
            String itemId = args[1];
            int amount, price;
            try {
                amount = Integer.parseInt(args[2]);
                price = Integer.parseInt(args[3]);
            } catch (Exception e) {
                player.sendMessage(textManager.getText("commands.market.usage_list", "§cUsage: /zmarket list <itemId> <amount> <price>"));
                return true;
            }
            plugin.getMarketManager().list(player, itemId, amount, price);
            player.sendMessage(String.format(textManager.getText("commands.market.listed_item", "§aListed %d x %s for %d coins."), amount, itemId, price));
            return true;
        }
        if (sub.equals("buy") && args.length >= 2) {
            int index;
            try {
                index = Integer.parseInt(args[1]);
            } catch (Exception e) {
                player.sendMessage(textManager.getText("commands.market.usage_buy", "§cUsage: /zmarket buy <index>"));
                return true;
            }
            if (plugin.getMarketManager().buy(player, index))
                player.sendMessage(String.format(textManager.getText("commands.market.purchased_listing", "§aPurchased listing #%d"), index));
            else
                player.sendMessage(textManager.getText("commands.market.could_not_buy", "§cCouldn't buy listing."));
            return true;
        }
        if (sub.equals("view")) {
            int i = 0;
            for (MarketManager.Listing l : plugin.getMarketManager().getListings()) {
                player.sendMessage(String.format(textManager.getText("commands.market.view_listing_format", "§7#%d §f%dx §e%s §7for §6%d §7coins"), i, l.amount, l.itemId, l.price));
                i++;
            }
            return true;
        }
        player.sendMessage(textManager.getText("commands.market.unknown_subcommand", "§cUnknown subcommand."));
        return true;
    }
}
