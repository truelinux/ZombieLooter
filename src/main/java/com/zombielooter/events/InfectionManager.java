package com.zombielooter.events;

import cn.nukkit.level.Level;
import cn.nukkit.scheduler.Task;
import com.zombielooter.ZombieLooterX;

public class InfectionManager {
    private final ZombieLooterX plugin;
    private int infectionLevel = 0; // 0..100

    public InfectionManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        startLoop();
    }

    private void startLoop() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new Task() {
            @Override public void onRun(int currentTick) {
                // Increase at night, decay at day
                for (Level level : plugin.getServer().getLevels().values()) {
                    long t = level.getTime() % 24000;
                    boolean isNight = t >= 13000 && t <= 23000;
                    if (isNight) increase(2);
                    else decrease(1);
                }
                // Trigger purge event at 100
                if (infectionLevel >= 100) {
                    plugin.getServer().broadcastMessage("§4☣ Global Infection reached 100%! Purge event begins!");
                    if (plugin.getZombieSpawner() != null) {
                        // spawn small hordes near all players
                        plugin.getZombieSpawner().spawnHordesNearAll(4, 6);
                    }
                    infectionLevel = 60; // cool down
                }
            }
        }, 20 * 20); // every 20s
    }

    public void increase(int n){ infectionLevel = Math.min(100, infectionLevel + n); }
    public void decrease(int n){ infectionLevel = Math.max(0, infectionLevel - n); }
    public int getInfectionLevel(){ return infectionLevel; }
}
