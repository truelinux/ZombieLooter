package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.form.window.CustomForm;
import cn.nukkit.form.element.custom.ElementInput;
import cn.nukkit.utils.TextFormat;
import com.zombielooter.ZombieLooterX;

/**
 * UIManager centralises titles, popups, action bars and lightweight forms.
 */
public class UIManager {
    private final ZombieLooterX plugin;

    public UIManager(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    /** Send a kill toast to a player with XP and coin rewards. */
    public void sendKillToast(Player p, String entityName, int xp, int coins) {
        p.sendTitle(color("&6Defeated " + entityName),
                color("&a+" + xp + " XP  &e+" + coins + " coins"), 5, 40, 5);
        p.getLevel().addParticleEffect(p, ParticleEffect.VILLAGER_HAPPY);
        p.getLevel().addSound(p, Sound.RANDOM_LEVELUP);
    }

    /** Display an infection warning via the action bar. */
    public void sendInfectionWarning(Player p, int percent) {
        p.sendActionBar(color("&4☣ Infection Level: " + percent + "%"));
    }

    /** Minimal list-item form used by older market flow. */
    private void openListItemForm(Player p) {
        CustomForm form = new CustomForm("List Item");
        form.addElement(new ElementInput("Item ID", "minecraft:diamond"));
        form.addElement(new ElementInput("Amount", "1"));
        form.addElement(new ElementInput("Price", "100"));
        form.onSubmit((player, resp) -> {
            String id = resp.getInputResponse(0);
            int amt;
            int price;
            try {
                amt = Integer.parseInt(resp.getInputResponse(1));
                price = Integer.parseInt(resp.getInputResponse(2));
            } catch (Exception e) {
                player.sendPopup(color("&cInvalid amount/price."));
                return;
            }
            plugin.getMarketManager().list(player, id, amt, price);
            player.sendTitle(color("&aListed Item"), "", 5, 40, 5);
        });
        form.send(p);
    }

    private String color(String msg) {
        return TextFormat.colorize('&', msg);
    }
}
