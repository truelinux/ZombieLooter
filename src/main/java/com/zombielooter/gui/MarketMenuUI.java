package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.level.Sound;
import cn.nukkit.form.window.SimpleForm;
import com.zombielooter.ZombieLooterX;
import java.util.List;

/**
 * Market menu GUI.
 * Driven by gui_text.yml under gui.market.*
 */
public final class MarketMenuUI {

    private MarketMenuUI() {}

    public static void openMainMenu(ZombieLooterX plugin, Player player) {
        GUITextManager text = plugin.getGUITextManager();

        String title = text.getTitle("market");
        String desc = text.getDescription("market");
        List<String> buttons = text.getButtons("market");

        SimpleForm form = new SimpleForm(title, desc);
        if (buttons != null) {
            for (String label : buttons) {
                form.addButton("§l" + label);
            }
        }

        form.send(player);
        player.getLevel().addSound(player, Sound.CLICK_ON_NETHER_WOOD_BUTTON);
    }

    static void handleMenu(ZombieLooterX plugin, Player player, int index) {
        switch (index) {
            case 0 -> player.getServer().executeCommand(player, "zmarket view");
            case 1 -> {
                FeedbackUtil.toast(player, "§6Selling Help", "§7Use /zmarket list <item> <amount> <price>");
                FeedbackUtil.actionBar(player, "§7Example: §f/zmarket list minecraft:iron_ingot 16 250");
            }
            case 2 -> FeedbackUtil.popup(player, "§7Closed market.");
            default -> FeedbackUtil.popup(player, "§7Exited.");
        }
        FeedbackUtil.actionBar(player, "§eMarket menu action executed");
    }
}


