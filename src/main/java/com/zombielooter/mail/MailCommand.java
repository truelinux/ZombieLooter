package com.zombielooter.mail;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;

public class MailCommand implements CommandExecutor {
    private final ZombieLooterX plugin;
    private final MailManager mailManager;

    public MailCommand(ZombieLooterX plugin, MailManager mailManager) {
        this.plugin = plugin;
        this.mailManager = mailManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("view")) {
            int i = 0;
            for (MailManager.MailEntry entry : mailManager.viewInbox(player.getUniqueId())) {
                player.sendMessage("§7#" + i + " §f" + entry.amount + "x " + entry.itemId);
                i++;
            }
            int overflow = mailManager.getOverflowCount(player.getUniqueId());
            if (overflow > 0) {
                player.sendMessage("§e+" + overflow + " more queued (inbox full). Claim items to reveal.");
            }
            if (i == 0) {
                player.sendMessage("§7Your mail is empty.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
                mailManager.claimAll(player);
                return true;
            }
            try {
                int idx = Integer.parseInt(args[1]);
                mailManager.claimSingle(player, idx);
            } catch (NumberFormatException e) {
                player.sendMessage("§cUsage: /mail claim <index|all>");
            }
            return true;
        }

        player.sendMessage("§cUsage: /mail [view|claim <index|all>]");
        return true;
    }
}
