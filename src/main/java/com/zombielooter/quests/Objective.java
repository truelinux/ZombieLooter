package com.zombielooter.quests;

import cn.nukkit.Player;

public interface Objective {
    String getId();
    String getDescription();
    boolean isComplete(QuestProgress progress);
    void onKill(Player killer, String mobType, QuestProgress progress);
}
