package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.form.element.custom.ElementDropdown;
import cn.nukkit.form.element.custom.ElementSlider;
import cn.nukkit.form.window.CustomForm;
import cn.nukkit.form.window.SimpleForm;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.Vendor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cn.nukkit.utils.TextFormat;

public class VendorMenuUI {

    public static void openVendorSelectionMenu(ZombieLooterX plugin, Player player) {
        SimpleForm form = new SimpleForm("Select a Vendor");
        for (Vendor vendor : plugin.getVendorManager().getVendors().values()) {
            form.addButton(vendor.getName());
        }
        form.send(player);
    }

    public static void openMainMenu(ZombieLooterX plugin, Player player, String vendorId) {
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) return;

        SimpleForm form = new SimpleForm(vendor.getName());
        form.addButton("Buy Items");
        form.addButton("Sell Items");
        form.send(player);
    }

    public static void openBuyMenu(ZombieLooterX plugin, Player player, String vendorId) {
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) return;

        CustomForm form = new CustomForm("Buy from " + vendor.getName());
        List<String> buyableItems = new ArrayList<>();
        for (String itemKey : vendor.getItemKeys()) {
            if (vendor.getItem(itemKey).isBuyable()) {
                buyableItems.add(vendor.getItem(itemKey).getName());
            }
        }
        form.addElement(new ElementDropdown("Item", buyableItems));
        form.addElement(new ElementSlider("Amount", 1, 64, 1));
        form.send(player);
    }

    public static void openSellMenu(ZombieLooterX plugin, Player player, String vendorId) {
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) return;

        CustomForm form = new CustomForm("Sell to " + vendor.getName());
        List<String> sellableItems = new ArrayList<>();
        for (String itemKey : vendor.getItemKeys()) {
            if (vendor.getItem(itemKey).isSellable()) {
                sellableItems.add(vendor.getItem(itemKey).getName());
            }
        }
        form.addElement(new ElementDropdown("Item", sellableItems));
        form.addElement(new ElementSlider("Amount", 1, 64, 1));
        form.send(player);
    }

    public static void handleVendorSelection(ZombieLooterX plugin, Player player, int index) {
        List<String> vendorIds = new ArrayList<>(plugin.getVendorManager().getVendors().keySet());
        String vendorId = vendorIds.get(index);
        openMainMenu(plugin, player, vendorId);
    }

    public static void handleMainMenu(ZombieLooterX plugin, Player player, String vendorId, int index) {
        if (index == 0) {
            openBuyMenu(plugin, player, vendorId);
        } else if (index == 1) {
            openSellMenu(plugin, player, vendorId);
        }
    }

    public static void handleBuyMenu(ZombieLooterX plugin, Player player, String vendorId, int itemIndex, int amount) {
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) return;

        List<String> buyableItemKeys = new ArrayList<>();
        for (String itemKey : vendor.getItemKeys()) {
            if (vendor.getItem(itemKey).isBuyable()) {
                buyableItemKeys.add(itemKey);
            }
        }
        String itemKey = buyableItemKeys.get(itemIndex);
        if (plugin.getVendorManager().buyItem(player, vendorId, itemKey, amount)) {
            player.sendMessage(TextFormat.colorize('&', "&aPurchase successful!"));
        } else {
            player.sendMessage(TextFormat.colorize('&', "&cPurchase failed."));
        }
    }

    public static void handleSellMenu(ZombieLooterX plugin, Player player, String vendorId, int itemIndex, int amount) {
        Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
        if (vendor == null) return;

        List<String> sellableItemKeys = new ArrayList<>();
        for (String itemKey : vendor.getItemKeys()) {
            if (vendor.getItem(itemKey).isSellable()) {
                sellableItemKeys.add(itemKey);
            }
        }
        String itemKey = sellableItemKeys.get(itemIndex);
        if (plugin.getVendorManager().sellItem(player, vendorId, itemKey, amount)) {
            player.sendMessage(TextFormat.colorize('&', "&aSale successful!"));
        } else {
            player.sendMessage(TextFormat.colorize('&', "&cSale failed."));
        }
    }
}
