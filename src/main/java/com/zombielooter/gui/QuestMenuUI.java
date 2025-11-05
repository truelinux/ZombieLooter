package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.level.Sound;
import cn.nukkit.form.window.SimpleForm;
import com.zombielooter.ZombieLooterX;
import java.util.List;

/**
 * Quest menu GUI.
 * Driven by gui_text.yml under gui.quest.*
 */
public final class QuestMenuUI {

    public static void openMainMenu(ZombieLooterX plugin, Player player) {
        GUITextManager text = plugin.getGUITextManager();

        String title = text.getTitle("quest");
        String desc = text.getDescription("quest");
        List<String> buttons = text.getButtons("quest");

        SimpleForm form = new SimpleForm(title, desc);
        if (buttons != null) {
            for (String label : buttons) {
                form.addButton("§l" + label);
            }
        }

        form.send(player);
        form.send(player);
        player.getLevel().addSound(player, Sound.CLICK_ON_NETHER_WOOD_BUTTON);
    }

    static void handleMenu(ZombieLooterX plugin, Player player, int index) {
        switch (index) {
            case 0 -> player.getServer().executeCommand(player, "quest list");
            case 1 -> player.getServer().executeCommand(player, "quest progress");
            case 2 -> player.getServer().executeCommand(player, "quest claim");
            case 3 -> player.getServer().executeCommand(player, "quest help");
            default -> FeedbackUtil.popup(player, "§7Menu closed.");
        }
        FeedbackUtil.actionBar(player, "§aQuest menu action executed");
    }
}

