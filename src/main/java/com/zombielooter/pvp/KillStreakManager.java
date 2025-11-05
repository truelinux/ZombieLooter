package com.zombielooter.pvp;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerDeathEvent;
import com.zombielooter.ZombieLooterX;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * KillStreakManager tracks kill streaks in PvP areas.  Every 5 kills awards
 * a bounty to the killer.  When a streak holder dies, their streak and
 * bounty are cleared.  Bounties can be claimed by killing players with
 * active bounties.
 */
public class KillStreakManager implements Listener {
    private final ZombieLooterX plugin;
    private final Map<UUID, Integer> streaks = new HashMap<>();
    private final Map<UUID, Integer> bounties = new HashMap<>();

    public KillStreakManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        // Register this as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Entity killer = victim.getKiller();
        // Only increment streaks if killer exists and is in a PvP area
        if (killer instanceof Player && killer != null && plugin.getZoneManager().isPvP(killer.getLocation())) {
            streaks.put(killer.getUniqueId(), streaks.getOrDefault(killer.getUniqueId(), 0) + 1);
            int streak = streaks.get(killer.getUniqueId());
            plugin.getUIManager().sendKillToast((Player) killer, victim.getName(), 5, 0);
            // Award bounty every 5 kills
            if (streak % 5 == 0) {
                int bounty = streak * 10;
                bounties.put(killer.getUniqueId(), bounty);
                plugin.getServer().broadcastMessage("§6Killstreak! " + killer.getName() +
                        " is on " + streak + " kills. Bounty: " + bounty + " coins.");
            }
        }
        // Reset victim's streak and bounty on death
        streaks.remove(victim.getUniqueId());
        bounties.remove(victim.getUniqueId());
    }

    /**
     * Get the current bounty on a player.
     *
     * @param p player
     * @return bounty value
     */
    public int getBounty(Player p) {
        return bounties.getOrDefault(p.getUniqueId(), 0);
    }

    /**
     * Claim the bounty on a target.  Awards the hunter coins equal to the
     * bounty amount and resets the bounty on the target.
     *
     * @param hunter the player claiming the bounty
     * @param target the player with the bounty
     */
    public void claimBounty(Player hunter, Player target) {
        int bounty = getBounty(target);
        if (bounty > 0) {
            plugin.getEconomyManager().addBalance(hunter.getUniqueId(), bounty);
            bounties.remove(target.getUniqueId());
            hunter.sendPopup("§aBounty claimed: " + bounty + " coins!");
        }
    }
}