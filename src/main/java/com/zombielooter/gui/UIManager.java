package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementHeader;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.form.window.CustomForm;
import cn.nukkit.form.window.SimpleForm;
import cn.nukkit.form.element.custom.ElementInput;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.factions.Faction;
import com.zombielooter.factions.FactionManager;

/**
 * UIManager centralises titles, popups, action bars and GUI menus.
 * It wraps ScoreboardAPI and the PNX Form API to provide a polished experience.
 */
public class UIManager {
    private final ZombieLooterX plugin;

    public UIManager(ZombieLooterX plugin) {
        this.plugin = plugin;
    }

    /**
     * Send a kill toast to a player with XP and coin rewards.
     * Also plays a levelup sound and happy particles at the player's position.
     *
     * @param p          player to notify
     * @param entityName name of the defeated entity
     * @param xp         XP gained
     * @param coins      coins gained
     */
    public void sendKillToast(Player p, String entityName, int xp, int coins) {
        p.sendTitle("§6Defeated " + entityName,
                "§a+" + xp + " XP  §e+" + coins + " coins", 5, 40, 5);
        p.getLevel().addParticleEffect(p, ParticleEffect.VILLAGER_HAPPY);
        p.getLevel().addSound(p, Sound.RANDOM_LEVELUP);
    }

    /**
     * Display an infection warning via the action bar.
     *
     * @param p       player to notify
     * @param percent infection level (0–100)
     */
    public void sendInfectionWarning(Player p, int percent) {
        p.sendActionBar("§4☣ Infection Level: " + percent + "%");
    }




//    /**
//     * Open the market menu.  Players can browse listings or list an item for sale.
//     *
//     * @param p player
//     */
//    public void openMarketMenu(Player p) {
//        SimpleForm form = new SimpleForm("Marketplace");
//        form.content("Choose an action:");
//        form.addButton("Browse Listings");
//        form.addButton("List an Item");
//        p.sendForm(p, form, (player, resp) -> {
//            if (!(resp instanceof FormResponseSimple)) return;
//            int idx = ((FormResponseSimple) resp).getClickedButtonId();
//            if (idx == 0) openBrowseMarket(player);
//            if (idx == 1) openListItemForm(player);
//        });
//    }

//    private void openBrowseMarket(Player p) {
//        MarketManager mm = plugin.getMarketManager();
//        SimpleForm form = new SimpleForm("Browse Listings");
//        form.setContent("Tap an item to buy:");
//        mm.getListings().forEach(l -> form.addButton(new ElementButton(
//                "§e" + l.amount + "x " + l.itemId + " §7for §6" + l.price + " coins")));
//        plugin.getServer().getFormManager().sendForm(p, form, (player, resp) -> {
//            if (!(resp instanceof FormResponseSimple)) return;
//            int idx = ((FormResponseSimple) resp).getClickedButtonId();
//            if (idx < 0 || idx >= mm.getListings().size()) return;
//            MarketManager.Listing listing = mm.getListings().get(idx);
//            CustomForm confirm = new CustomForm("Confirm Purchase");
//            confirm.addElement(new ElementLabel(
//                    "Buy " + listing.amount + "x " + listing.itemId + " for " + listing.price + " coins?"));
//            confirm.onSubmit((pl, r) -> {
//                if (plugin.getMarketManager().buy(pl, idx)) {
//                    pl.sendTitle("§aPurchase Successful", "", 5, 40, 5);
//                } else {
//                    pl.sendPopup("§cPurchase failed.");
//                }
//            });
//            confirm.send(player);
//        });
//    }

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
                player.sendPopup("§cInvalid amount/price.");
                return;            }
            plugin.getMarketManager().list(player, id, amt, price);
            player.sendTitle("§aListed Item", "", 5, 40, 5);
        });
        form.send(p);
    }
}