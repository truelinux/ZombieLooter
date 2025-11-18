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
            sender.sendMessage(TextFormat.RED + "Players only.");
            return true;
        }

        Player player = (Player) sender;
        dailyRewardManager.claimDailyReward(player);
        return true;
    }
}
