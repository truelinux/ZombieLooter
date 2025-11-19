package com.zombielooter.market;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.gui.MarketMenuUI;
import cn.nukkit.utils.TextFormat;

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
            sender.sendMessage(TextFormat.colorize('&', textManager.getText("commands.market.only_players", "&cPlayers only.")));
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
                player.sendMessage(TextFormat.colorize('&', textManager.getText("commands.market.usage_list", "&cUsage: /zmarket list <itemId> <amount> <price>")));
                return true;
            }
            plugin.getMarketManager().list(player, itemId, amount, price);
            player.sendMessage(TextFormat.colorize('&', String.format(textManager.getText("commands.market.listed_item", "&aListed %d x %s for %d coins."), amount, itemId, price)));
            return true;
        }
        if (sub.equals("listbuy") && args.length >= 4) {
            String itemId = args[1];
            int amount, price;
            try {
                amount = Integer.parseInt(args[2]);
                price = Integer.parseInt(args[3]);
            } catch (Exception e) {
                player.sendMessage(TextFormat.colorize('&', "&cUsage: /zmarket listbuy <itemId> <amount> <price>"));
                return true;
            }
            if (plugin.getMarketManager().listBuy(player, itemId, amount, price)) {
                player.sendMessage(TextFormat.colorize('&', "&aPosted buy order. Funds reserved until filled (max 6h)."));
            } else {
                player.sendMessage(TextFormat.colorize('&', "&cNot enough funds to post that buy order."));
            }
            return true;
        }
        if (sub.equals("buy") && args.length >= 2) {
            int index;
            try {
                index = Integer.parseInt(args[1]);
            } catch (Exception e) {
                player.sendMessage(TextFormat.colorize('&', textManager.getText("commands.market.usage_buy", "&cUsage: /zmarket buy <index>")));
                return true;
            }
            if (plugin.getMarketManager().buy(player, index))
                player.sendMessage(TextFormat.colorize('&', String.format(textManager.getText("commands.market.purchased_listing", "&aPurchased listing #%d"), index)));
            else
                player.sendMessage(TextFormat.colorize('&', textManager.getText("commands.market.could_not_buy", "&cCouldn't buy listing.")));
            return true;
        }
        if (sub.equals("sell") && args.length >= 2) {
            int index;
            try {
                index = Integer.parseInt(args[1]);
            } catch (Exception e) {
                player.sendMessage(TextFormat.colorize('&', "&cUsage: /zmarket sell <index>"));
                return true;
            }
            if (plugin.getMarketManager().sellToBuyListing(player, index))
                player.sendMessage(TextFormat.colorize('&', "&aSold into the buy order. Payment delivered."));
            else
                player.sendMessage(TextFormat.colorize('&', "&cCould not sell into that buy order."));
            return true;
        }
        if (sub.equals("view")) {
            int page = 0;
            if (args.length >= 2) {
                try { page = Integer.parseInt(args[1]) - 1; } catch (Exception ignored) {}
            }
            plugin.getMarketManager().openListingInventory(player, page);
            return true;
        }
        player.sendMessage(TextFormat.colorize('&', textManager.getText("commands.market.unknown_subcommand", "&cUnknown subcommand.")));
        return true;
    }
}
