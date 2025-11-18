package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.rewards.DailyRewardManager;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

import java.util.UUID;

public class DailyRewardsCommand implements CommandExecutor {

    private final DailyRewardManager dailyRewardManager;
    private final ZombieLooterX plugin;

    public DailyRewardsCommand(ZombieLooterX plugin, DailyRewardManager dailyRewardManager) {
        this.plugin = plugin;
        this.dailyRewardManager = dailyRewardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.RED + "Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "status":
                    dailyRewardManager.showStatus(player);
                    return true;
                case "reset":
                    if (!sender.hasPermission("zombielooter.dailyrewards.admin")) {
                        sender.sendMessage(TextFormat.colorize('&', "&cNo permission."));
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(TextFormat.colorize('&', "&eUsage: /dailyrewards reset <player>"));
                        return true;
                    }
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        dailyRewardManager.resetProgress(target.getUniqueId());
                        sender.sendMessage(TextFormat.colorize('&', "&aReset daily reward progress for &f" + target.getName()));
                        return true;
                    }
                    try {
                        UUID id = UUID.fromString(args[1]);
                        dailyRewardManager.resetProgress(id);
                        sender.sendMessage(TextFormat.colorize('&', "&aReset daily reward progress."));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(TextFormat.colorize('&', "&cPlayer must be online or provide a UUID."));
                    }
                    return true;
                default:
                    // fall back to claim
            }
        }

        dailyRewardManager.claimDailyReward(player);
        return true;
    }
}
