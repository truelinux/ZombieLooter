package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.scoreboard.data.DisplaySlot;
import cn.nukkit.scoreboard.manager.IScoreboardManager;
import com.zombielooter.ZombieLooterX;
import me.skh6075.pnx.graphicscore.GraphicScore;
import me.skh6075.pnx.graphicscore.player.ScorePlayer;
import me.skh6075.pnx.graphicscore.player.ScorePlayerManager;

import java.util.Objects;

public class HUDManager {
    public static String ZOMBIELOOTER = "zombielooter_hud";

    private final IScoreboardManager manager = ZombieLooterX.instance.getServer().getScoreboardManager();  // Hypothetical API call

    public void showHUD(Player player, String id) {
        if(manager.containScoreboard(id)) {
            Objects.requireNonNull(manager.getScoreboard(id)).addViewer(player, DisplaySlot.SIDEBAR);
        }
    }

    public void clearHUD(Player player, String score) {
        player.removeScoreboard(manager.getScoreboard(score));
    }

    public static void refreshHud(ZombieLooterX plugin, Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        ScorePlayer session = ScorePlayerManager.INSTANCE.getPlayer(player);
        if (session == null) {
            return;
        }

        session.update("zombielooter_hud:faction_name");
        session.update("zombielooter_hud:power");
        session.update("zombielooter_hud:xp");
        session.update("zombielooter_hud:balance");
        session.update("zombielooter_hud:prefix");

        session.changeScoreboard(
                GraphicScore.Companion.getInstance().getBoard(HUDManager.ZOMBIELOOTER)
        );
    }
}
