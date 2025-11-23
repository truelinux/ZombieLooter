package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.rewards.DailyRewardManager;
import cn.nukkit.utils.TextFormat;

public class DailyRewardsCommand implements CommandExecutor {

    private final DailyRewardManager dailyRewardManager;

    public DailyRewardsCommand(DailyRewardManager dailyRewardManager) {
        this.dailyRewardManager = dailyRewardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.colorize('&', TextFormat.RED + "Players only."));
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("spin")) {
                dailyRewardManager.spinDailyWheel(player);
                return true;
            }
            if (sub.equals("reward")) {
                dailyRewardManager.claimDailyReward(player);
                return true;
            }
            player.sendMessage(TextFormat.colorize('&', "&eUsage: /" + label + " reward|spin"));
            return true;
        }
        dailyRewardManager.claimDailyReward(player);
        return true;
    }
}
