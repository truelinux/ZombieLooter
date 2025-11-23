package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import cn.nukkit.form.element.ElementHeader;
import cn.nukkit.form.element.custom.ElementDropdown;
import cn.nukkit.form.element.custom.ElementInput;
import cn.nukkit.form.element.custom.ElementSlider;
import cn.nukkit.form.window.CustomForm;
import cn.nukkit.form.window.SimpleForm;
import cn.nukkit.utils.TextFormat;
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
                form.addButton(TextFormat.colorize('&', "&l" + label));
            }
        }

        form.send(player);
        player.getLevel().addSound(player, Sound.CLICK_ON_NETHER_WOOD_BUTTON);
    }

    static void handleMenu(ZombieLooterX plugin, Player player, int index) {
        switch (index) {
            case 0 -> player.getServer().executeCommand(player, "zmarket view");
            case 1 -> openListingCreator(plugin, player);
            case 2 -> {
                FeedbackUtil.toast(player, "&6Selling Help", "&7Use /zmarket list <item> <amount> <price>");
                FeedbackUtil.actionBar(player, "&7Example: &f/zmarket list minecraft:iron_ingot 16 250");
            }
            default -> FeedbackUtil.popup(player, "&7Exited.");
        }
        player.getLevel().addSound(player, Sound.CLICK_OFF_NETHER_WOOD_BUTTON);
    }

    private static void openListingCreator(ZombieLooterX plugin, Player player) {
        GUITextManager text = plugin.getGUITextManager();
        Item hand = player.getInventory().getItemInHand();
        if (hand == null || hand.getId() == Item.AIR.getId()) {
            FeedbackUtil.toast(player, "&cNeed Item", text.get("gui.form_market_create.missing_item", "&7Hold the item you want to list."));
            return;
        }
        String title = text.get("gui.form_market_create.title", "Create Market Listing");
        CustomForm form = new CustomForm(title);
        String holding = String.format(text.get("gui.form_market_create.holding", "&7Holding: &f%s x%d"), describe(hand), hand.getCount());
        form.addElement(new ElementHeader(TextFormat.colorize('&', holding)));
        List<String> options = List.of(
                text.get("gui.form_market_create.type_sell", "Sell Listing"),
                text.get("gui.form_market_create.type_buy", "Buy Order")
        );
        form.addElement(new ElementDropdown(text.get("gui.form_market_create.dropdown", "Listing Type"), options));
        int sliderMax = Math.max(hand.getCount(), hand.getMaxStackSize() * 4);
        sliderMax = Math.max(sliderMax, 1);
        int defaultValue = Math.min(hand.getCount(), sliderMax);
        form.addElement(new ElementSlider(text.get("gui.form_market_create.amount", "Amount"), 1, sliderMax, 1, defaultValue));
        form.addElement(new ElementInput(text.get("gui.form_market_create.price", "Total Price (coins)"), "500", "500"));
        form.send(player);
    }

    private static String describe(Item item) {
        if (item == null) return "Unknown";
        if (item.hasCustomName()) {
            return item.getCustomName();
        }
        String name = item.getName();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return item.getIdentifier() != null ? item.getIdentifier().toString() : "Item";
    }
}


