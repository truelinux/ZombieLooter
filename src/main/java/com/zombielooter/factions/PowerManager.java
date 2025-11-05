package com.zombielooter.factions;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;

public class PowerManager implements Listener {
    private final ZombieLooterX plugin;
    private final FactionManager fm;
    private cn.nukkit.utils.Config powerCfg;

    public PowerManager(ZombieLooterX plugin, FactionManager fm){
        this.plugin = plugin; this.fm = fm; load();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void load(){
        File f = new File(plugin.getDataFolder(), "power.yml");
        if (!f.exists()) plugin.saveResource("power.yml", false);
        powerCfg = new Config(f, Config.YAML);
    }

    public int getPower(String faction){ return powerCfg.getInt(faction.toLowerCase(), 0); }
    public void addPower(String faction, int delta){
        String key = faction.toLowerCase();
        int cur = powerCfg.getInt(key, 0);
        powerCfg.set(key, Math.max(0, cur + delta)); powerCfg.save();
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e){
        if (!(e.getEntity() instanceof Player) && e.getEntity().getLastDamageCause().getEntity() instanceof Player) return;
        Player victim = (Player) e.getEntity();
        Player killer = (Player)victim.getKiller();
        if (killer != null){
            Faction fk = fm.getFactionByPlayer(killer.getUniqueId());
            if (fk != null) addPower(fk.getName(), plugin.getConfig().getInt("factions.power.kill_player", 5));
        }
        Faction fv = fm.getFactionByPlayer(victim.getUniqueId());
        if (fv != null) addPower(fv.getName(), -plugin.getConfig().getInt("factions.power.death_penalty", 3));
    }
}
