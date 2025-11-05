package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;

public class EconomyCommand implements CommandExecutor {

    private final ZombieLooterX plugin;

    public EconomyCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /eco <bal|give|take> [player] [amount]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "bal":
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    sender.sendMessage("§aBalance: §e" + plugin.getEconomyManager().getBalance(p.getUniqueId()));
                } else sender.sendMessage("§cPlayers only.");
                break;

            case "give":
            case "take":
                if (!sender.hasPermission("zombielooter.economy.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§eUsage: /eco " + args[0] + " <player> <amount>");
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid amount.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("give")) {
                    plugin.getEconomyManager().addBalance(target.getUniqueId(), amount);
                    sender.sendMessage("§aGave §e" + amount + "§a to " + target.getName());
                } else {
                    if (plugin.getEconomyManager().withdraw(target.getUniqueId(), amount))
                        sender.sendMessage("§aTook §e" + amount + "§a from " + target.getName());
                    else sender.sendMessage("§cInsufficient funds.");
                }
                break;

            default:
                sender.sendMessage("§eUsage: /eco <bal|give|take> [player] [amount]");
        }
        return true;
    }
}
