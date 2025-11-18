package com.zombielooter.quests;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.gui.QuestMenuUI;

import java.util.Map;

public class QuestCommand implements CommandExecutor {

    private final ZombieLooterX plugin;
    private final GUITextManager textManager;

    public QuestCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.textManager = plugin.getGUITextManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(textManager.getText("commands.quest.only_players", "§cOnly players can use quest commands."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            QuestMenuUI.openMainMenu(plugin, player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                for (Quest q : plugin.getQuestManager().getAll()) {
                    player.sendMessage("§6" + q.getId() + "§7: §f" + q.getName() + " §8(Reward: §e" + q.getRewardCoins() + "§8)");
                }
                break;

            case "start":
                if (args.length < 2) {
                    player.sendMessage(textManager.getText("commands.quest.usage_start", "§eUsage: /quest start <id>"));
                    return true;
                }
                Quest quest = plugin.getQuestManager().getQuest(args[1]);
                if (quest == null) {
                    player.sendMessage(textManager.getText("commands.quest.quest_not_found", "§cQuest not found."));
                    return true;
                }
                plugin.getQuestManager().getProgress(player, quest.getId());
                sender.sendMessage(textManager.getText("commands.quest.started_quest_prefix", "§aStarted quest: §e") + quest.getName());
                break;

            case "progress":
                Map<String, QuestProgress> playerQuests = plugin.getQuestManager()
                    .getPlayerProgress(player.getUniqueId());

                if (playerQuests.isEmpty()) {
                    player.sendMessage(textManager.getText("commands.quest.no_active_quests", "§7You have no active quests. Use §e/quest list§7 to see available quests."));
                    return true;
                }

                player.sendMessage(textManager.getText("commands.quest.active_quests_header", "§6=== Your Active Quests ==="));
                for (Map.Entry<String, QuestProgress> entry : playerQuests.entrySet()) {
                    Quest q = plugin.getQuestManager().getQuest(entry.getKey());
                    if (q == null) continue;

                    player.sendMessage("§e" + q.getName() + ":");
                    for (Objective obj : q.getObjectives()) {
                        int current = entry.getValue().getCounter(obj.getId());
                        player.sendMessage("  §7- " + obj.getDescription() + " §8[" + current + "]");
                    }
                }
                break;

            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(textManager.getText("commands.quest.help_header", "§6==== §lQuest Help §r§6===="));
        p.sendMessage(textManager.getText("commands.quest.help_line1", "§7- Start earning awards by completing Quests."));
        p.sendMessage(textManager.getText("commands.quest.help_line2", "§7- Quests refresh daily - use /quest"));
    }
}
