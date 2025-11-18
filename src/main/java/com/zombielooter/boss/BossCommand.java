package com.zombielooter.boss;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.BossMenuUI;
import com.zombielooter.gui.GUITextManager;

public class BossCommand implements CommandExecutor {

    private final ZombieLooterX plugin;
    private final GUITextManager textManager;

    public BossCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.textManager = plugin.getGUITextManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(textManager.getText("commands.boss.only_players", "§cOnly players can use boss commands."));
            return true;
        }
        Player player = (Player) sender;
        if (!sender.hasPermission("zombielooter.boss")) {
            sender.sendMessage(textManager.getText("commands.boss.no_permission", "§cNo permission."));
            return true;
        }

        if (args.length == 0) {
            BossMenuUI.openMainMenu(plugin, player );
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(textManager.getText("commands.boss.available_bosses", "§6Available bosses:"));
            for (String id : plugin.getBossEventManager().listBosses()) {
                sender.sendMessage("§7 - §f" + id);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                sender.sendMessage(textManager.getText("commands.boss.usage_spawn", "§eUsage: /boss spawn <id>"));
                return true;
            }
            if (plugin.getBossEventManager().trigger(args[1]))
                sender.sendMessage(textManager.getText("commands.boss.boss_spawned", "§aBoss spawned successfully!"));
            else sender.sendMessage(textManager.getText("commands.boss.boss_not_found", "§cBoss not found or failed to spawn."));
            return true;
        }

        sender.sendMessage(textManager.getText("commands.boss.usage", "§e/boss <list|spawn <id>>"));
        return true;
    }
}
