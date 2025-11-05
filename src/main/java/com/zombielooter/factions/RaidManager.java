package com.zombielooter.factions;

import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;
import java.io.File;
import java.time.LocalTime;

public class RaidManager {
    private final ZombieLooterX plugin;
    private LocalTime start;
    private LocalTime end;
    public RaidManager(ZombieLooterX plugin){ this.plugin = plugin; load(); }
    private void load(){
        File f = new File(plugin.getDataFolder(), "raid.yml");
        if (!f.exists()) plugin.saveResource("raid.yml", false);
        Config cfg = new Config(f, Config.YAML);
        start = LocalTime.parse(cfg.getString("raid_window.start", "18:00"));
        end = LocalTime.parse(cfg.getString("raid_window.end", "22:00"));
    }
    public boolean isRaidTime(){
        LocalTime now = LocalTime.now();
        if (start.isBefore(end)) return !now.isBefore(start) && !now.isAfter(end);
        else return !now.isBefore(start) || !now.isAfter(end);
    }
}
