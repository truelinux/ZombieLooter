package com.zombielooter.gui;

import cn.nukkit.utils.Config;
import com.zombielooter.ZombieLooterX;

import java.io.File;
import java.util.List;

/**
 * Loads configurable GUI text (titles, descriptions, buttons) and
 * now also GUI themes from gui_text.yml.
 *
 * Expected structure:
 *
 * gui:
 *   boss:
 *     theme: "CRIMSON"
 *     title: "Boss Arena"
 *     description: "Face terrifying bosses..."
 *     buttons:
 *       - "List Bosses"
 *       - "Join"
 *       - "Rewards"
 *       - "Leave"
 */
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
        return "§l" + config.getString("gui." + key + ".title", "§lMenu");
    }

    public String getDescription(String key) {
        return config.getString("gui." + key + ".description", "§7No description.");
    }

    @SuppressWarnings("unchecked")
    public List<String> getButtons(String key) {
        return (List<String>) config.getList("gui." + key + ".buttons");
    }

}


