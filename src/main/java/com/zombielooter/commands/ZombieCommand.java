package com.zombielooter.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import com.zombielooter.ZombieLooterX;

/**
 * Command executor for /zlx
 * Usage:
 *   /zlx reload                - Reloads plugin configuration
 *   /zlx spawn <count> [here]  - Spawns a zombie horde
 */
public class ZombieCommand implements CommandExecutor {

    private final ZombieLooterX plugin;

    public ZombieCommand(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zombielooter.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /zlx <reload|spawn>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getZombieSpawner().reloadConfigCommand(sender);
                sender.sendMessage("§aConfiguration reloaded successfully!");
                return true;

            case "spawn":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use this command!");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§eUsage: /zlx spawn <count> [here]");
                    return true;
                }

                int count;
                try {
                    count = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number!");
                    return true;
                }

                Player player = (Player) sender;
                boolean spawnHere = args.length >= 3 && args[2].equalsIgnoreCase("here");

                if (spawnHere) {
                    for (int i = 0; i < count; i++) {
                        plugin.getZombieSpawner().spawnZombie(player.getLevel(), player.getPosition(), "§4Spawned Zombie");
                    }
                    player.getLevel().addSound(player.getPosition(), Sound.MOB_ZOMBIE_SAY);
                    player.getLevel().addParticleEffect(player.getPosition(), ParticleEffect.EXPLOSION_LABTABLE_FIRE);
                    player.sendMessage("§aSpawned " + count + " zombies at your position!");
                } else {
                    plugin.getZombieSpawner().spawnHordeRandom(player.getLevel(), player, count);
                    player.getLevel().addSound(player.getPosition(), Sound.MOB_ZOMBIE_SAY);
                    player.getLevel().addParticleEffect(player.getPosition(), ParticleEffect.CAMPFIRE_SMOKE_TALL);
                    player.sendMessage("§aSpawned a horde of " + count + " zombies nearby!");
                }
                return true;

            default:
                sender.sendMessage("§cUnknown subcommand. Use /zlx <reload|spawn>");
                return true;
        }
    }
}
