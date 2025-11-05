package com.zombielooter.quests;

import cn.nukkit.Player;

public class KillObjective implements Objective {
    private final String id; private final String mob; private final int required;
    public KillObjective(String id, String mob, int required){ this.id=id; this.mob=mob; this.required=required; }
    @Override public String getId(){ return id; }
    @Override public String getDescription(){ return "Kill "+required+" "+mob; }
    @Override public boolean isComplete(QuestProgress progress){ return progress.getCounter(id) >= required; }
    @Override public void onKill(Player killer, String mobType, QuestProgress progress){ if (mob.equalsIgnoreCase(mobType)) progress.increment(id, 1); }
}
