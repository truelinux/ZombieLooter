package com.zombielooter.commands;

import cn.nukkit.IPlayer;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.factions.Faction;
import com.zombielooter.gui.FactionMenuUI;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.gui.HUDManager;

import java.util.UUID;

public class FactionCommand implements CommandExecutor {
    private final ZombieLooterX plugin;
    private final GUITextManager text;

    public FactionCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
        this.text = plugin.getGUITextManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (args.length == 0) {
            FactionMenuUI.openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        Faction playerFaction = plugin.getFactionManager().getFactionByPlayer(uuid);

        switch (sub) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(text.getText("commands.faction.usage_create", "§eUsage: /f create <name>"));
                    return true;
                }
                if (playerFaction != null) {
                    player.sendMessage(text.getText("commands.faction.already_in_faction", "§cYou are already in a faction."));
                    return true;
                }
                if (plugin.getFactionManager().createFaction(player, args[1])) {
                    player.sendMessage(String.format(text.getText("commands.faction.create_success", "§aFaction '%s' created successfully!"), args[1]));
                    HUDManager.refreshHud(plugin, player);
                } else {
                    player.sendMessage(text.getText("commands.faction.create_fail_exists", "§cA faction with that name already exists."));
                }
                break;

            case "disband":
                if (playerFaction == null) {
                    player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                    return true;
                }
                if (!playerFaction.getLeader().equals(uuid)) {
                    player.sendMessage(text.getText("commands.faction.leader_only", "§cOnly the faction leader can do this."));
                    return true;
                }
                plugin.getServer().broadcastMessage(String.format(text.getText("commands.faction.disband_success", "§cThe faction '%s' has been disbanded!"), playerFaction.getName()));
                plugin.getFactionManager().disband(playerFaction.getName());
                break;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage(text.getText("commands.faction.usage_invite", "§eUsage: /f invite <player>"));
                    return true;
                }
                if (playerFaction == null) {
                    player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                    return true;
                }
                if (!playerFaction.getLeader().equals(uuid)) {
                    player.sendMessage(text.getText("commands.faction.leader_only", "§cOnly the faction leader can do this."));
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(text.getText("commands.faction.player_not_found", "§cPlayer not found."));
                    return true;
                }
                if (plugin.getFactionManager().getFactionByPlayer(target.getUniqueId()) != null) {
                    player.sendMessage(text.getText("commands.faction.invite_fail_in_faction", "§cThat player is already in a faction."));
                    return true;
                }
                playerFaction.invite(target.getUniqueId());
                player.sendMessage(String.format(text.getText("commands.faction.invite_sent", "§aInvitation sent to %s."), target.getName()));
                target.sendMessage(String.format(text.getText("commands.faction.invite_received", "§6You have been invited to join '%s'. Type §e/f join %s§6 to accept."), playerFaction.getName(), playerFaction.getName()));
                break;

            case "join":
                if (args.length < 2) {
                    player.sendMessage(text.getText("commands.faction.usage_join", "§eUsage: /f join <faction_name>"));
                    return true;
                }
                if (playerFaction != null) {
                    player.sendMessage(text.getText("commands.faction.already_in_faction", "§cYou are already in a faction."));
                    return true;
                }
                Faction factionToJoin = plugin.getFactionManager().getFaction(args[1]);
                if (factionToJoin == null) {
                    player.sendMessage(text.getText("commands.faction.faction_not_found", "§cFaction not found."));
                    return true;
                }
                if (factionToJoin.acceptInvite(uuid)) {
                    player.sendMessage(String.format(text.getText("commands.faction.join_success", "§aYou have joined '%s'!"), factionToJoin.getName()));
                    String joinBroadcast = String.format(text.getText("commands.faction.join_broadcast", "§e%s has joined the faction."), player.getName());
                    factionToJoin.getMembers().forEach(memberUUID -> plugin.getServer().getPlayer(memberUUID).ifPresent(p -> p.sendMessage(joinBroadcast)));
                    HUDManager.refreshHud(plugin, player);
                } else {
                    player.sendMessage(text.getText("commands.faction.join_fail_no_invite", "§cYou have not been invited to this faction."));
                }
                break;

            case "leave":
                if (playerFaction == null) {
                    player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                    return true;
                }
                if (playerFaction.getLeader().equals(uuid)) {
                    player.sendMessage(text.getText("commands.faction.leave_fail_leader", "§cThe leader cannot leave the faction. Use /f disband instead."));
                    return true;
                }
                String leftFactionName = playerFaction.getName();
                String leaveBroadcast = String.format(text.getText("commands.faction.leave_broadcast", "§e%s has left the faction."), player.getName());
                playerFaction.getMembers().forEach(memberUUID -> plugin.getServer().getPlayer(memberUUID).ifPresent(p -> p.sendMessage(leaveBroadcast)));
                playerFaction.removeMember(uuid);
                player.sendMessage(String.format(text.getText("commands.faction.leave_success", "§7You have left '%s'."), leftFactionName));
                HUDManager.refreshHud(plugin, player);
                break;

            case "kick":
                if (args.length < 2) {
                    player.sendMessage(text.getText("commands.faction.usage_kick", "§eUsage: /f kick <player>"));
                    return true;
                }
                if (playerFaction == null) {
                    player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                    return true;
                }
                if (!playerFaction.getLeader().equals(uuid)) {
                    player.sendMessage(text.getText("commands.faction.leader_only", "§cOnly the faction leader can do this."));
                    return true;
                }
                Player targetToKick = plugin.getServer().getPlayer(args[1]);
                if (targetToKick == null || !playerFaction.isMember(targetToKick.getUniqueId())) {
                    player.sendMessage(text.getText("commands.faction.kick_fail_not_in_faction", "§cThat player is not in your faction."));
                    return true;
                }
                if (targetToKick.getUniqueId().equals(uuid)) {
                    player.sendMessage(text.getText("commands.faction.kick_fail_self", "§cYou cannot kick yourself."));
                    return true;
                }
                playerFaction.removeMember(targetToKick.getUniqueId());
                player.sendMessage(String.format(text.getText("commands.faction.kick_success", "§aYou have kicked %s from the faction."), targetToKick.getName()));
                targetToKick.sendMessage(String.format(text.getText("commands.faction.kicked_notification", "§cYou have been kicked from '%s'."), playerFaction.getName()));
                HUDManager.refreshHud(plugin, targetToKick);
                break;

            case "info":
                Faction f = (args.length >= 2) ? plugin.getFactionManager().getFaction(args[1]) : playerFaction;
                if (f == null) {
                    player.sendMessage(text.getText("commands.faction.faction_not_found", "§cFaction not found."));
                    return true;
                }
                player.sendMessage(String.format(text.getText("commands.faction.info_header", "§6=== §eFaction: %s §6==="), f.getName()));
                
                // ** THE FIX IS HERE **
                // Correctly handle the nullable IPlayer object.
                IPlayer leader = plugin.getServer().getOfflinePlayer(f.getLeader());
                String leaderName = (leader != null) ? leader.getName() : "Unknown";
                player.sendMessage(String.format(text.getText("commands.faction.info_leader", "§7Leader: §e%s"), leaderName));

                player.sendMessage(String.format(text.getText("commands.faction.info_members", "§7Members: §f%d"), f.getMembers().size()));
                player.sendMessage(String.format(text.getText("commands.faction.info_power", "§7Power: §e%d / %d"), f.getPower(), plugin.getPowerManager().getMaxPower()));
                player.sendMessage(String.format(text.getText("commands.faction.info_land", "§7Land Claimed: §f%d"), plugin.getClaimManager().getClaimCount(f.getName())));
                break;

            case "status":
                if (playerFaction == null) {
                    player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                    return true;
                }
                int claimCount = plugin.getClaimManager().getClaimCount(playerFaction.getName());
                int landValue = claimCount * plugin.getPowerManager().getPowerPerClaim();
                boolean isRaidable = plugin.getRaidManager().isFactionRaidable(playerFaction.getName());
                player.sendMessage(String.format(text.getText("commands.faction.status_header", "§6=== §eFaction Status: %s §6==="), playerFaction.getName()));
                player.sendMessage(String.format(text.getText("commands.faction.status_power", "§7Current Power: §e%d"), playerFaction.getPower()));
                player.sendMessage(String.format(text.getText("commands.faction.status_land_value", "§7Land Value (Claims * %d): §f%d"), plugin.getPowerManager().getPowerPerClaim(), landValue));
                String raidableStatus = isRaidable ? text.getText("commands.faction.status_raidable_true", "§c§lRAIDABLE") : text.getText("commands.faction.status_raidable_false", "§a§lSECURE");
                player.sendMessage(String.format(text.getText("commands.faction.status_raidable", "§7Raidable Status: %s"), raidableStatus));
                if (!isRaidable) {
                    int safePower = landValue + plugin.getPowerManager().getRaidablePowerBuffer();
                    player.sendMessage(String.format(text.getText("commands.faction.status_raidable_info", "§7(Your power must drop below §c%d§7 to become raidable)"), safePower));
                }
                break;

            case "list":
                player.sendMessage(text.getText("commands.faction.list_header", "§6=== §eActive Factions §6==="));
                for (Faction fac : plugin.getFactionManager().getFactions()) {
                    player.sendMessage(String.format(text.getText("commands.faction.list_entry", "§7- §e%s §8(§7%d members, %d power§8)"), fac.getName(), fac.getMembers().size(), fac.getPower()));
                }
                break;

            case "sethome":
                if (playerFaction == null) {
                    player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                    return true;
                }
                if (!playerFaction.getLeader().equals(uuid)) {
                    player.sendMessage(text.getText("commands.faction.leader_only", "§cOnly the faction leader can do this."));
                    return true;
                }
                playerFaction.setHome(player.getLocation());
                player.sendMessage(text.getText("commands.faction.sethome_success", "§aFaction home set to your current location."));
                break;

            case "home":
                if (playerFaction == null || playerFaction.getHome() == null) {
                    player.sendMessage(text.getText("commands.faction.home_fail_no_home", "§cYour faction does not have a home set."));
                    return true;
                }
                player.teleport(playerFaction.getHome());
                player.sendMessage(text.getText("commands.faction.home_success", "§aTeleporting to faction home..."));
                break;

            case "raid":
                if (args.length > 1 && args[1].equalsIgnoreCase("banner")) {
                    if (playerFaction == null) {
                        player.sendMessage(text.getText("commands.faction.raid_banner_fail_faction", "§cYou must be in a faction to get a raid banner."));
                        return true;
                    }
                    if (!playerFaction.getLeader().equals(uuid)) {
                        player.sendMessage(text.getText("commands.faction.raid_banner_fail_leader", "§cOnly the faction leader can get a raid banner."));
                        return true;
                    }
                    int cost = plugin.getRaidManager().getBannerCost();
                    if (plugin.getEconomyManager().withdraw(uuid, cost)) {
                        player.getInventory().addItem(plugin.getRaidManager().getRaidBanner());
                        player.sendMessage(String.format(text.getText("commands.faction.raid_banner_success", "§aYou have purchased a Raid Banner for §e%d coins."), cost));
                    } else {
                        player.sendMessage(String.format(text.getText("commands.faction.raid_banner_fail_money", "§cYou cannot afford a Raid Banner. It costs %d coins."), cost));
                    }
                } else {
                    player.sendMessage(text.getText("commands.faction.raid_banner_usage", "§eUsage: /f raid banner"));
                }
                break;

            case "bank":
                if (playerFaction == null) {
                    player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                    return true;
                }
                player.sendMessage(String.format(text.getText("commands.faction.bank_balance", "§6Faction Bank Balance: §e%.2f coins"), playerFaction.getBankBalance()));
                break;

            case "deposit":
                if (args.length < 2) {
                    player.sendMessage(text.getText("commands.faction.deposit_usage", "§eUsage: /f deposit <amount>"));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) {
                        player.sendMessage(text.getText("commands.faction.deposit_fail_positive", "§cPlease enter a positive amount."));
                        return true;
                    }
                    if (playerFaction == null) {
                        player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                        return true;
                    }
                    if (plugin.getEconomyManager().withdraw(uuid, (int) amount)) {
                        playerFaction.depositToBank(amount);
                        player.sendMessage(String.format(text.getText("commands.faction.deposit_success", "§aYou deposited §e%.2f§a coins into the faction bank."), amount));
                    } else {
                        player.sendMessage(text.getText("commands.faction.deposit_fail_money", "§cYou do not have enough money to deposit."));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(text.getText("commands.faction.invalid_amount", "§cInvalid amount specified."));
                }
                break;

            case "withdraw":
                if (args.length < 2) {
                    player.sendMessage(text.getText("commands.faction.withdraw_usage", "§eUsage: /f withdraw <amount>"));
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) {
                        player.sendMessage(text.getText("commands.faction.deposit_fail_positive", "§cPlease enter a positive amount."));
                        return true;
                    }
                    if (playerFaction == null) {
                        player.sendMessage(text.getText("commands.faction.not_in_faction", "§cYou are not in a faction."));
                        return true;
                    }
                    if (!playerFaction.getLeader().equals(uuid)) {
                        player.sendMessage(text.getText("commands.faction.withdraw_fail_leader", "§cOnly the faction leader can withdraw from the bank."));
                        return true;
                    }
                    if (playerFaction.withdrawFromBank(amount)) {
                        plugin.getEconomyManager().addBalance(uuid, (int) amount);
                        player.sendMessage(String.format(text.getText("commands.faction.withdraw_success", "§aYou withdrew §e%.2f§a coins from the faction bank."), amount));
                    } else {
                        player.sendMessage(text.getText("commands.faction.withdraw_fail_money", "§cThe faction bank does not have enough money."));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(text.getText("commands.faction.invalid_amount", "§cInvalid amount specified."));
                }
                break;

            default:
                player.sendMessage(text.getText("commands.faction.unknown_subcommand", "§cUnknown subcommand. Type /f for help."));
                break;
        }
        return true;
    }
}
