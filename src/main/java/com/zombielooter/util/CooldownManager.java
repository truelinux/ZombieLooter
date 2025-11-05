package com.zombielooter.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<String, Long> map = new HashMap<>();
    private String key(UUID id, String action){ return id.toString()+":"+action; }
    public boolean ready(UUID id, String action, long millis){
        long now = System.currentTimeMillis();
        String k = key(id, action);
        Long until = map.getOrDefault(k, 0L);
        if (now >= until){ map.put(k, now + millis); return true; }
        return false;
    }
    public long remaining(UUID id, String action){
        long now = System.currentTimeMillis();
        return Math.max(0, map.getOrDefault(key(id, action), 0L) - now);
    }
    public void clear(UUID id, String action){ map.remove(key(id, action)); }
}
