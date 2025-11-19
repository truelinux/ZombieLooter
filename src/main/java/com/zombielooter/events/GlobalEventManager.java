package com.zombielooter.events;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.scheduler.Task;
import com.zombielooter.ZombieLooterX;

import java.util.Random;
import cn.nukkit.utils.TextFormat;

public class GlobalEventManager {
    private final ZombieLooterX plugin;
    private final Random random = new Random();
    private boolean bloodMoon = false;
    private boolean fog = false;

    public GlobalEventManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        startEventLoop();
    }

    private void startEventLoop() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, new Task() {
            @Override public void onRun(int currentTick) {
                // small chance to toggle events at night
                for (Level level : plugin.getServer().getLevels().values()) {
                    long time = level.getTime() % 24000;
                    boolean isNight = time >= 13000 && time <= 23000;
                    if (!isNight) continue;

                    if (!bloodMoon && random.nextDouble() < 0.01) startBloodMoon(level);
                    if (!fog && random.nextDouble() < 0.08) startFog(level);

                    if (fog) {
                        for (Player p : level.getPlayers().values()) {
                            for (int i = 0; i < 5; i++) {
                                level.addParticleEffect(p.add( (random.nextDouble()-0.5)*2, 1, (random.nextDouble()-0.5)*2 ), ParticleEffect.CAMPFIRE_SMOKE);
                            }
                        }
                    }
                }
            }
        }, 200);
    }

    private void startBloodMoon(Level level) {
        bloodMoon = true;
        plugin.getConfig().set("horde.spawn_chance", plugin.getConfig().getDouble("horde.spawn_chance", 0.07) * 2.0);
        plugin.saveConfig();
        broadcast("&4☾ Blood Moon rises! Hordes are more frequent tonight...");
        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
            bloodMoon = false;
            // restore default by halving back (simple)
            plugin.getConfig().set("horde.spawn_chance", plugin.getConfig().getDouble("horde.spawn_chance", 0.07) / 2.0);
            plugin.saveConfig();
            broadcast("&7☽ Dawn breaks. The Blood Moon fades.");
        }, 20 * 60 * 6); // 6 minutes
    }

    private void startFog(Level level) {
        fog = true;
        broadcast("&7🌫 A thick fog rolls in...");
        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
            fog = false;
            broadcast("&7🌤 The fog clears.");
        }, 20 * 60 * 4);
    }

    private void broadcast(String msg) {
        for (Player p : plugin.getServer().getOnlinePlayers().values()) p.sendMessage(TextFormat.colorize('&', msg));
    }

    public boolean isBloodMoon() { return bloodMoon; }
    public boolean isFog() { return fog; }
}
