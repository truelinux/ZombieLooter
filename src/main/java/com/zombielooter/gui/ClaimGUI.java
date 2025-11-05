package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.form.window.ModalForm;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.factions.Faction;

/**
 * ClaimGUI shows a confirmation modal for claiming land.  It is intended to
 * be used from a command or UI element to confirm chunk claims.  If the
 * player is not in a faction or not the leader, an error is sent.
 */
public class ClaimGUI {

    public static void openMainMenu(ZombieLooterX plugin, Player player) {
        Faction f = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        if (f == null) {
            player.sendPopup("§cNot in a faction.");
            return;
        }
        ModalForm form = new ModalForm("Claim Land", "Do you want to claim and flatten this chunk?\nCosts 1 power point.")
                .text("§aConfirm", "§cCancel")
                .onYes(p -> plugin.getServer().executeCommand(p, "f claim confirm"))
                .onNo(p -> player.sendPopup("§7Claim cancelled."));
        form.send(player);
    }
}