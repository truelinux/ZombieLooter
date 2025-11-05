package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.BossMenuUI;

public class BossCommand implements CommandExecutor {

    private final ZombieLooterX plugin;

    public BossCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use boss commands.");
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("zombielooter.boss")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            BossMenuUI.openMainMenu(plugin, player );
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§6Available bosses:");
            for (String id : plugin.getBossEventManager().listBosses()) {
                sender.sendMessage("§7 - §f" + id);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                sender.sendMessage("§eUsage: /boss spawn <id>");
                return true;
            }
            if (plugin.getBossEventManager().trigger(args[1]))
                sender.sendMessage("§aBoss spawned successfully!");
            else sender.sendMessage("§cBoss not found or failed to spawn.");
            return true;
        }

        sender.sendMessage("§e/boss <list|spawn <id>>");
        return true;
    }
}
