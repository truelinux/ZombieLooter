package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.form.window.SimpleForm;
import cn.nukkit.level.Sound;
import com.zombielooter.ZombieLooterX;
import java.util.List;

/**
 * Faction main menu.
 * All text and the theme are driven by gui_text.yml:
 *
 * gui:
 *   faction:
 *     theme: "FACTION"
 *     title: "Faction Menu"
 *     description: "Manage your land and members."
 *     buttons:
 *       - "Info"
 *       - "Claim Preview"
 *       - "Confirm Claim"
 *       - "Unclaim"
 *       - "Leave Faction"
 *       - "Disband"
 */
public final class FactionMenuUI {

    private FactionMenuUI() {}

    public static void openMainMenu(ZombieLooterX plugin, Player player) {
        GUITextManager text = plugin.getGUITextManager();

        String title = text.getTitle("faction");
        String desc = text.getDescription("faction");
        List<String> buttons = text.getButtons("faction");

        SimpleForm form = new SimpleForm(title, desc);
        if (buttons != null) {
            for (String label : buttons) {
                form.addButton("§l" + label);
            }
        }

        form.send(player);
        player.getLevel().addSound(player, Sound.CLICK_ON_NETHER_WOOD_BUTTON);
    }

    static void handleMenu(ZombieLooterX plugin, Player player, String title, int index) {
        switch (index) {
            case 0 -> player.getServer().executeCommand(player, "f info");
            case 1 -> player.getServer().executeCommand(player, "f claim preview");
            case 2 -> ClaimGUI.openMainMenu(plugin, player);
            case 3 -> player.getServer().executeCommand(player, "f unclaim");
            case 4 -> player.getServer().executeCommand(player, "f leave");
            case 5 -> player.getServer().executeCommand(player, "f disband");
            default -> FeedbackUtil.popup(player, "§7Menu closed.");
        }
        player.getLevel().addSound(player, Sound.CLICK_OFF_NETHER_WOOD_BUTTON);
    }
}


