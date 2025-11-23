package com.zombielooter.gui;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GUITextManager {
    private final Config config;

    public GUITextManager(ZombieLooterX plugin) {
        File file = new File(plugin.getDataFolder(), "gui_text.yml");
        if (!file.exists()) {
            plugin.saveResource("gui_text.yml", false);
        }
        this.config = new Config(file, Config.YAML);
    }

    public String getTitle(String key) {
        return colorize("&l" + config.getString("gui." + key + ".title", "&lMenu"));
    }

    public String getDescription(String key) {
        return colorize(config.getString("gui." + key + ".description", "&7No description."));
    }

    public List<String> getButtons(String key) {
        List<String> raw = (List<String>) config.getList("gui." + key + ".buttons");
        if (raw == null) return null;
        List<String> colored = new ArrayList<>(raw.size());
        for (String s : raw) {
            colored.add(colorize(s));
        }
        return colored;
    }

    public String getText(String key, String defaultValue) {
        return colorize(config.getString("gui." + key, defaultValue));
    }

    public String get(String key, String defaultValue) {
        return colorize(config.getString(key, defaultValue));
    }

    private String colorize(String value) {
        if (value == null) return null;
        return TextFormat.colorize('&', value.replace("\u0015", "&").replace("§", "&"));
    }
}
