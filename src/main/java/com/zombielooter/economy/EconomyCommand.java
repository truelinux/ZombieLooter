package com.zombielooter.economy;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;

public class EconomyCommand implements CommandExecutor {

    private final ZombieLooterX plugin;
    private final GUITextManager textManager;

    public EconomyCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.textManager = plugin.getGUITextManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(textManager.getText("commands.economy.usage", "§eUsage: /eco <bal|give|take> [player] [amount]"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "bal":
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    sender.sendMessage(textManager.getText("commands.economy.balance_prefix", "§aBalance: §e") + plugin.getEconomyManager().getBalance(p.getUniqueId()));
                } else sender.sendMessage(textManager.getText("commands.economy.only_players", "§cPlayers only."));
                break;

            case "give":
            case "take":
                if (!sender.hasPermission("zombielooter.economy.admin")) {
                    sender.sendMessage(textManager.getText("commands.economy.no_permission", "§cNo permission."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(String.format(textManager.getText("commands.economy.usage_admin", "§eUsage: /eco %s <player> <amount>"), args[0]));
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(textManager.getText("commands.economy.player_not_found", "§cPlayer not found."));
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(textManager.getText("commands.economy.invalid_amount", "§cInvalid amount."));
                    return true;
                }
                if (args[0].equalsIgnoreCase("give")) {
                    plugin.getEconomyManager().addBalance(target.getUniqueId(), amount);
                    sender.sendMessage(String.format(textManager.getText("commands.economy.gave_coins", "§aGave §e%d§a to %s"), amount, target.getName()));
                } else {
                    if (plugin.getEconomyManager().withdraw(target.getUniqueId(), amount))
                        sender.sendMessage(String.format(textManager.getText("commands.economy.took_coins", "§aTook §e%d§a from %s"), amount, target.getName()));
                    else sender.sendMessage(textManager.getText("commands.economy.insufficient_funds", "§cInsufficient funds."));
                }
                break;

            default:
                sender.sendMessage(textManager.getText("commands.economy.usage", "§eUsage: /eco <bal|give|take> [player] [amount]"));
        }
        return true;
    }
}
