package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.level.Sound;
import cn.nukkit.form.window.SimpleForm;
import com.zombielooter.ZombieLooterX;
import java.util.List;

/**
 * Boss event GUI.
 * Uses BaseGUI + GUITextManager + ThemeManager.
 */
public final class BossMenuUI {

    private BossMenuUI() {}

    public static void openMainMenu(ZombieLooterX plugin, Player player) {
        GUITextManager text = plugin.getGUITextManager();

        String title = text.getTitle("boss");
        String desc = text.getDescription("boss");
        List<String> buttons = text.getButtons("boss");

        SimpleForm form = new SimpleForm(title, desc);
        if (buttons != null) {
            for (String label : buttons) {
                form.addButton("§l" + label);
            }
        }

        form.send(player);
        player.getLevel().addSound(player, Sound.MOB_SHULKER_AMBIENT);
    }

    static void handleMenu(ZombieLooterX plugin, Player player, int index) {
        switch (index) {
            case 0 -> player.getServer().executeCommand(player, "boss list");
            case 1 -> player.getServer().executeCommand(player, "boss join");
            case 2 -> player.getServer().executeCommand(player, "boss rewards");
            case 3 -> player.getServer().executeCommand(player, "boss leave");
            case 4 -> FeedbackUtil.popup(player, "§7Menu closed.");
            default -> FeedbackUtil.popup(player, "§7Exited.");
        }
        FeedbackUtil.actionBar(player, "§dBoss menu action executed");
    }
}


