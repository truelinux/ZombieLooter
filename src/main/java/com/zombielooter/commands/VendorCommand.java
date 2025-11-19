package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.VendorMenuUI;
import cn.nukkit.utils.TextFormat;

public class VendorCommand implements CommandExecutor {

    private final ZombieLooterX plugin;

    public VendorCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.colorize('&', "&cThis command can only be used by players."));
            return true;
        }

        VendorMenuUI.openVendorSelectionMenu(plugin, (Player) sender);
        return true;
    }
}
