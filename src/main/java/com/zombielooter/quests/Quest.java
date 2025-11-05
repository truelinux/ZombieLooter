package com.zombielooter.quests;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    private final String id; private final String name; private final List<Objective> objectives = new ArrayList<>(); private final long rewardCoins;
    public Quest(String id, String name, long rewardCoins){ this.id=id; this.name=name; this.rewardCoins=rewardCoins; }
    public String getId(){ return id; }
    public String getName(){ return name; }
    public long getRewardCoins(){ return rewardCoins; }
    public List<Objective> getObjectives(){ return objectives; }
    public Quest addObjective(Objective o){ objectives.add(o); return this; }
    public boolean isComplete(QuestProgress progress){ for (Objective o: objectives) if (!o.isComplete(progress)) return false; return true; }
}
