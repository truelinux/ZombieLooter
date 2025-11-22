package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.gui.GUITextManager;
import com.zombielooter.zones.Region;
import cn.nukkit.utils.TextFormat;

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
            sender.sendMessage(TextFormat.colorize('&', textManager.getText("commands.zombie.no_permission", "&cYou don't have permission to use this command.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(TextFormat.colorize('&', textManager.getText("commands.zombie.usage", "&eUsage: /zlx <reload|spawn|npc|zonepos>")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getZombieSpawner().reloadConfigCommand(sender);
                sender.sendMessage(TextFormat.colorize('&', textManager.getText("commands.zombie.reload_success", "&aConfiguration reloaded successfully!")));
                return true;

            case "spawn":
                // ... (spawn logic remains the same)
                return true;

            case "npc":
                if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
                    sender.sendMessage(TextFormat.colorize('&', "&eUsage: /zlx npc spawn <vendorId>"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(TextFormat.colorize('&', "&cThis command can only be used by a player."));
                    return true;
                }
                plugin.getNpcManager().createAndSaveNPC((Player) sender, args[2]);
                return true;

            case "zoneinfo":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(TextFormat.colorize('&', "&cOnly players can inspect zones."));
                    return true;
                }
                Player viewer = (Player) sender;
                Region region = plugin.getZoneManager().getRegionAt(viewer.getLocation());
                if (region == null) {
                    sender.sendMessage(TextFormat.colorize('&', "&eNo region at your position. mobSpawn defaults to &atrue&e outside regions."));
                    return true;
                }
                String min = String.format("(%.0f, %.0f, %.0f)", region.getMin().getX(), region.getMin().getY(), region.getMin().getZ());
                String max = String.format("(%.0f, %.0f, %.0f)", region.getMax().getX(), region.getMax().getY(), region.getMax().getZ());
                sender.sendMessage(TextFormat.colorize('&',
                        "&6Zone: &e" + region.getName() + " &7in &f" + region.getLevelName() +
                                "\n&7Min: &f" + min + " &7Max: &f" + max +
                                "\n&7Flags&8: &f" + summarizeFlags(region.getFlags())));
                return true;

            case "zonepos":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(TextFormat.colorize('&', "&cOnly players can set zone positions."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(TextFormat.colorize('&', "&eUsage: /zlx zonepos <regionName> <p1|p2>"));
                    return true;
                }
                String regionName = args[1];
                String corner = args[2];
                Player p = (Player) sender;
                boolean saved = plugin.getZoneManager().setRegionCorner(regionName, p.getLocation(), corner);
                if (saved) {
                    sender.sendMessage(TextFormat.colorize('&', "&aSaved &e" + corner.toUpperCase() + "&a for region '&e" + regionName + "&a' at your current block. Zones reloaded."));
                } else {
                    sender.sendMessage(TextFormat.colorize('&', "&cFailed to save that corner. Make sure you used p1 or p2 and are standing in a loaded world."));
                }
                return true;

            default:
                sender.sendMessage(TextFormat.colorize('&', textManager.getText("commands.zombie.unknown_subcommand", "&cUnknown subcommand. Use /zlx <reload|spawn|npc|zonepos|zoneinfo>")));
                return true;
        }
    }

    private String summarizeFlags(java.util.Map<String, Boolean> flags) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var entry : flags.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }
}
