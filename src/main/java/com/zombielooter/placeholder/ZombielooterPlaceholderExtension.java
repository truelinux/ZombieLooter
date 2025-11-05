package com.zombielooter.placeholder;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.plugin.Plugin;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.factions.Faction;
import com.zombielooter.gui.HUDManager;
import me.skh6075.pnx.graphicscore.GraphicScore;
import me.skh6075.pnx.graphicscore.placeholder.PlaceholderExtension;
import me.skh6075.pnx.graphicscore.player.ScorePlayer;
import me.skh6075.pnx.graphicscore.player.ScorePlayerManager;
import org.jetbrains.annotations.NotNull;


public class ZombielooterPlaceholderExtension extends PlaceholderExtension {

    @Override
    public String onRequest(Player player, String params) {
        ZombieLooterX plugin = (ZombieLooterX) getOwnedPlugin();
        Faction fac = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        String facName = fac != null ? fac.getName() : "---";
        String facPower = fac != null ? String.valueOf(fac.getPower()) : "---";
        return switch (params) {
            case "faction_name" -> facName;
            case "power" -> facPower;
            case "xp" -> String.valueOf(plugin.getXPManager().getXP(player.getUniqueId()));
            case "balance" -> String.valueOf(plugin.getEconomyManager().getBalance(player.getUniqueId()));
            case "prefix" -> ZombieLooterX.PREFIX;
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e) {
        ScorePlayerManager.INSTANCE.getPlayer(e.getPlayer()).changeScoreboard(GraphicScore.Companion.getInstance().getBoard(HUDManager.ZOMBIELOOTER));
        onPlaceholderUpdate(e.getPlayer());
    }

    public void onPlaceholderUpdate(Player player) {
        ScorePlayer session = ScorePlayerManager.INSTANCE.getPlayer(player);
        if (session != null) {
            session.update("zombielooter_hud:faction_name");
            session.update("zombielooter_hud:power");
            session.update("zombielooter_hud:xp");
            session.update("zombielooter_hud:balance");
            session.update("zombielooter_hud:prefix");
            ScorePlayerManager.INSTANCE.getPlayer(player).changeScoreboard(GraphicScore.Companion.getInstance().getBoard(HUDManager.ZOMBIELOOTER));
        }

    }

    @Override
    public @NotNull Plugin getOwnedPlugin() {
        return ZombieLooterX.instance;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zombielooter_hud";
    }
}

