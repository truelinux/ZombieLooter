package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.market.MarketManager;
import cn.nukkit.Player;
import com.zombielooter.gui.MarketMenuUI;
import com.zombielooter.gui.FactionMenuUI;


public class MarketCommand implements CommandExecutor {
    private final ZombieLooterX plugin;
    public MarketCommand(ZombieLooterX plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length == 0) {
            MarketMenuUI.openMainMenu(plugin, player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("list") && args.length >= 4) {
            String itemId = args[1];
            int amount, price;
            try { amount = Integer.parseInt(args[2]); price = Integer.parseInt(args[3]); }
            catch (Exception e){ player.sendMessage("§cUsage: /zmarket list <itemId> <amount> <price>"); return true; }
            plugin.getMarketManager().list(player, itemId, amount, price);
            player.sendMessage("§aListed " + amount + "x " + itemId + " for " + price + " coins.");
            return true;
        }
        if (sub.equals("buy") && args.length >= 2) {
            int index;
            try { index = Integer.parseInt(args[1]); } catch (Exception e){ player.sendMessage("§cUsage: /zmarket buy <index>"); return true; }
            if (plugin.getMarketManager().buy(player, index)) player.sendMessage("§aPurchased listing #" + index);
            else player.sendMessage("§cCouldn't buy listing.");
            return true;
        }
        if (sub.equals("view")) {
            int i = 0;
            for (MarketManager.Listing l : plugin.getMarketManager().getListings()) {
                player.sendMessage("§7#"+i+" §f"+l.amount+"x §e"+l.itemId+" §7for §6"+l.price+" §7coins");
                i++;
            }
            return true;
        }
        player.sendMessage("§cUnknown subcommand.");
        return true;
    }
}
