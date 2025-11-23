package com.zombielooter.util;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Periodically broadcasts short helper tips to the whole server to guide players.
 * Tips are loaded from tips.yml and rotate every intervalSeconds.
 */
public class TipManager {
    private final ZombieLooterX plugin;
    private final Random random = new Random();
    private List<String> tips = new ArrayList<>();
    private int intervalSeconds = 240;
    private int taskId = -1;

    public TipManager(ZombieLooterX plugin) {
        this.plugin = plugin;
        load();
        start();
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), "tips.yml");
        if (!file.exists()) {
            plugin.saveResource("tips.yml", false);
        }
        Config cfg = new Config(file, Config.YAML);
        intervalSeconds = cfg.getInt("interval-seconds", 240);
        tips = cfg.getStringList("tips");
        if (tips == null) {
            tips = new ArrayList<>();
        }
    }

    private void start() {
        stop();
        if (tips.isEmpty()) return;
        taskId = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> {
            String tip = tips.get(random.nextInt(tips.size()));
            plugin.getServer().broadcastMessage(TextFormat.colorize('&', "&6[Tip] &f" + tip));
        }, intervalSeconds * 20).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
