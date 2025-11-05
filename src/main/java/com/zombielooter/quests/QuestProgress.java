package com.zombielooter.quests;

import java.util.HashMap;
import java.util.Map;

public class QuestProgress {
    private final Map<String,Integer> counters = new HashMap<>();
    public int getCounter(String key){ return counters.getOrDefault(key, 0); }
    public void increment(String key, int by){ counters.put(key, getCounter(key)+by); }
    public Map<String,Integer> getAll(){ return counters; }
}
