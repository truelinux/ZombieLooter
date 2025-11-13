package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;

public class ZombieCommand implements CommandExecutor {

    private final ZombieLooterX plugin;
    private final GUITextManager textManager;

    public ZombieCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.textManager = plugin.getGUITextManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zombielooter.admin")) {
            sender.sendMessage(textManager.getText("commands.zombie.no_permission", "§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(textManager.getText("commands.zombie.usage", "§eUsage: /zlx <reload|spawn|npc>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getZombieSpawner().reloadConfigCommand(sender);
                sender.sendMessage(textManager.getText("commands.zombie.reload_success", "§aConfiguration reloaded successfully!"));
                return true;

            case "spawn":
                // ... (spawn logic remains the same)
                return true;

            case "npc":
                if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
                    sender.sendMessage("§eUsage: /zlx npc spawn <vendorId>");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by a player.");
                    return true;
                }
                plugin.getNpcManager().createAndSaveNPC((Player) sender, args[2]);
                return true;

            default:
                sender.sendMessage(textManager.getText("commands.zombie.unknown_subcommand", "§cUnknown subcommand. Use /zlx <reload|spawn|npc>"));
                return true;
        }
    }
}
