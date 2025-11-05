package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.factions.Faction;
import cn.nukkit.Player;
import com.zombielooter.gui.HUDManager;
import com.zombielooter.gui.FactionMenuUI;


import java.util.UUID;

public class FactionCommand implements CommandExecutor {
    private final ZombieLooterX plugin;
    public FactionCommand(ZombieLooterX plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {  return true; }
        UUID uuid = player.getUniqueId();
        if (args.length == 0) {
            // Open GUI for players instead of plain help
            FactionMenuUI.openMainMenu(plugin, player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("create")) {
            if (args.length < 2) { 
                player.sendMessage("§eUsage: /f create <name>"); 
                return true; 
            }
            
            String factionName = args[1];
            if (factionName.length() < 3 || factionName.length() > 16) {
                player.sendMessage("§cFaction name must be 3-16 characters.");
                return true;
            }
            if (!factionName.matches("[a-zA-Z0-9_]+")) {
                player.sendMessage("§cFaction name can only contain letters, numbers, and underscores.");
                return true;
            }
            
            if (plugin.getFactionManager().getFactionByPlayer(uuid) != null) {
                player.sendMessage("§cYou're already in a faction.");
                return true;
            }
            if (!plugin.getFactionManager().createFaction(player, args[1])) {
                player.sendMessage("§cFaction exists.");
                return true;
            }
            player.sendMessage("§aFaction created.");
            HUDManager.refreshHud(plugin, player);
            return true;
        }

        if (sub.equals("disband")) {
            if (!player.hasPermission("zlx.faction.admin")) { player.sendMessage("§cNo permission."); return true; }
            Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
            if (f == null) { player.sendMessage("§cYou're not in a faction."); return true; }
            if (!f.getLeader().equals(uuid)) { player.sendMessage("§cLeader only."); return true; }
            plugin.getFactionManager().disband(f.getName());
            player.sendMessage("§cFaction disbanded.");
            HUDManager.refreshHud(plugin, player);
            return true;
        }

    // NEW: Invite command (leader only)
    if (sub.equals("invite")) {
        Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
        if (f == null) { 
            player.sendMessage("§cYou're not in a faction."); 
            return true; 
        }
        if (!f.getLeader().equals(uuid)) { 
            player.sendMessage("§cOnly the leader can invite players."); 
            return true; 
        }
        if (args.length < 2) { 
            player.sendMessage("§eUsage: /f invite <player>"); 
            return true; 
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) { 
            player.sendMessage("§cPlayer not found or offline."); 
            return true; 
        }
        
        if (plugin.getFactionManager().getFactionByPlayer(target.getUniqueId()) != null) {
            player.sendMessage("§cThat player is already in a faction.");
            return true;
        }
        
        f.invite(target.getUniqueId());
        plugin.getFactionManager().save();
        
        player.sendMessage("§aYou invited §e" + target.getName() + " §ato your faction.");
        target.sendMessage("§6You've been invited to faction §e" + f.getName());
        target.sendMessage("§aUse §e/f join " + f.getName() + " §ato accept!");
        return true;
    }

    // MODIFIED: Join command (now requires invitation)
    if (sub.equals("join")) {
        if (args.length < 2) { 
            player.sendMessage("§eUsage: /f join <name>"); 
            return true; 
        }
        
        if (plugin.getFactionManager().getFactionByPlayer(uuid) != null) { 
            player.sendMessage("§cYou're already in a faction."); 
            return true; 
        }
        
        Faction target = plugin.getFactionManager().getFaction(args[1]);
        if (target == null) { 
            player.sendMessage("§cFaction not found."); 
            return true; 
        }
        
        // NEW: Check for invitation
        if (!target.hasInvite(uuid)) {
            player.sendMessage("§cYou don't have an invitation to that faction!");
            player.sendMessage("§7Ask a leader to invite you with §e/f invite " + player.getName());
            return true;
        }
        
        // Accept the invitation
        target.acceptInvite(uuid);
        plugin.getFactionManager().save();
        
        player.sendMessage("§aYou joined faction §e" + target.getName());
        
        // Notify online faction members
        for (UUID memberId : target.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId).orElse(null);
            if (member != null && member.isOnline()) {
                member.sendMessage("§e" + player.getName() + " §ajoined the faction!");
            }
        }
        
        HUDManager.refreshHud(plugin, player);
        return true;
    }

    // NEW: Kick command (leader only)
    if (sub.equals("kick")) {
        Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
        if (f == null) { 
            player.sendMessage("§cYou're not in a faction."); 
            return true; 
        }
        if (!f.getLeader().equals(uuid)) { 
            player.sendMessage("§cOnly the leader can kick members."); 
            return true; 
        }
        if (args.length < 2) { 
            player.sendMessage("§eUsage: /f kick <player>"); 
            return true; 
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) { 
            player.sendMessage("§cPlayer not found."); 
            return true; 
        }
        
        if (!f.isMember(target.getUniqueId())) {
            player.sendMessage("§cThat player is not in your faction.");
            return true;
        }
        
        if (f.getLeader().equals(target.getUniqueId())) {
            player.sendMessage("§cYou cannot kick yourself! Use /f disband instead.");
            return true;
        }
        
        f.removeMember(target.getUniqueId());
        plugin.getFactionManager().save();
        
        player.sendMessage("§cYou kicked §e" + target.getName() + " §cfrom the faction.");
        target.sendMessage("§cYou were kicked from faction §e" + f.getName());
        HUDManager.refreshHud(plugin, target);
        return true;
    }

        if (sub.equals("leave")) {
            Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
            if (f == null) { player.sendMessage("§cYou're not in a faction."); return true; }
            if (f.getLeader().equals(uuid)) { player.sendMessage("§cLeaders must /f disband."); return true; }
            f.removeMember(uuid); plugin.getFactionManager().save();
            player.sendMessage("§7You left your faction.");
            HUDManager.refreshHud(plugin, player);
            return true;
        }

        if (sub.equals("info")) {
            Faction f = (args.length >= 2) ? plugin.getFactionManager().getFaction(args[1])
                    : plugin.getFactionManager().getFactionByPlayer(uuid);
            if (f == null) { player.sendMessage("§cFaction not found."); return true; }
            String leaderName = plugin.getServer().getOfflinePlayer(f.getLeader()).getName();
            player.sendMessage("§6=== §eFaction: " + f.getName() + " §6===");
            player.sendMessage("§7Leader: §e" + leaderName + " §7Power: §e" + f.getPower());
            player.sendMessage("§7Members: §f" + f.getMembers().size());
            return true;
        }

        if (sub.equals("list")) {
            player.sendMessage("§6Active Factions:");
            for (Faction f : plugin.getFactionManager().getFactions()) {
                player.sendMessage("§7- §e" + f.getName() + " §8(§7Members:§f " + f.getMembers().size() + "§8, §7Power: §f" + f.getPower() + "§8)");
            }
            return true;
        }

        // Homes
        if (sub.equals("sethome")) {
            Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
            if (f == null) { player.sendMessage("§cYou're not in a faction."); return true; }
            if (!f.getLeader().equals(uuid)) { player.sendMessage("§cLeader only."); return true; }
            f.setHome(player.getLocation()); plugin.getFactionManager().save();
            player.sendMessage("§aFaction home set.");
            return true;
        }
        if (sub.equals("home")) {
            Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
            if (f == null || f.getHome() == null) { player.sendMessage("§cYour faction has no home."); return true; }
            player.teleport(f.getHome());
            player.sendMessage("§aTeleported to faction home.");
            return true;
        }

        // Claims
        if (sub.equals("claim")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("preview")) { plugin.getClaimManager().previewClaimChunk(player); return true; }
            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
                if (f == null) { player.sendMessage("§cNot in a faction."); return true; }
                if (!f.getLeader().equals(uuid)) { player.sendMessage("§cLeader only."); return true; }
                plugin.getClaimManager().claimChunk(f, player); return true;
            }
            player.sendMessage("§eUsage: /f claim preview|confirm");
            return true;
        }

        if (sub.equals("unclaim")) {
            Faction f = plugin.getFactionManager().getFactionByPlayer(uuid);
            if (f == null) { player.sendMessage("§cNot in a faction."); return true; }
            if (!f.getLeader().equals(uuid)) { player.sendMessage("§cLeader only."); return true; }
            plugin.getClaimManager().unclaimChunk(f, player);
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Type /f for help.");
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage("§6==== §lFaction Commands §r§6====");
        p.sendMessage("§e/f create <name> §7- Create a faction");
        p.sendMessage("§e/f disband §7- Disband your faction §8(leader)");
        p.sendMessage("§e/f invite <player> §7- Invite a player §8(leader)");
        p.sendMessage("§e/f join <name> §7- Accept an invitation");
        p.sendMessage("§e/f kick <player> §7- Kick a member §8(leader)");
        p.sendMessage("§e/f leave §7- Leave your faction");
        p.sendMessage("§e/f info [name] §7- Faction info");
        p.sendMessage("§e/f list §7- List factions");
        p.sendMessage("§e/f sethome §7- Set faction home §8(leader)");
        p.sendMessage("§e/f home §7- Teleport to home");
        p.sendMessage("§e/f claim preview §7- Show claim border");
        p.sendMessage("§e/f claim confirm §7- Flatten & claim §8(leader)");
        p.sendMessage("§e/f unclaim §7- Unclaim current chunk §8(leader)");
    }
}
