package com.zombielooter.kitpvp;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;

/**
 * /kitpvp join|leave|leaderboard command.
 */
public class KitPvpCommand implements CommandExecutor {

    private final KitPvpManager manager;

    public KitPvpCommand(KitPvpManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player player = (Player) sender;
        String sub = args.length > 0 ? args[0].toLowerCase() : "join";
        switch (sub) {
            case "leave":
            case "return":
                manager.leave(player);
                break;
            case "leaderboard":
                manager.showLeaderboard(player);
                break;
            default:
                manager.join(player);
                break;
        }
        return true;
    }
}
