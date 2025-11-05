package com.zombielooter.leaderboard;

import cn.nukkit.Player;

import cn.nukkit.scoreboard.Scoreboard;
import com.zombielooter.ZombieLooterX;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LeaderboardManager constructs and displays leaderboards, such as the
 * top balances (eco top).  It uses the sidebar scoreboard to render the
 * leaderboard on the client.
 */
public class LeaderboardManager {
    private final ZombieLooterX plugin;

    public LeaderboardManager(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

}