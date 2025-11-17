package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.response.CustomResponse;
import cn.nukkit.form.response.SimpleResponse;
import cn.nukkit.form.window.CustomForm;
import cn.nukkit.form.window.SimpleForm;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.economy.Vendor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIFormListener implements Listener {

    private final ZombieLooterX plugin;
    private final Map<UUID, Long> formCooldown = new HashMap<>();
    private final Map<UUID, String> playerVendorInteraction = new HashMap<>();

    public GUIFormListener(ZombieLooterX plugin) {
        this.plugin = plugin;
        FactionMenuUI.init(plugin);
    }

    @EventHandler
    public void onFormRespond(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        // ** THE FIX IS HERE **
        // Removed the incorrect isCancelled() check.
        if (player == null || !player.isOnline()) return;

        long now = System.currentTimeMillis();
        if (now - formCooldown.getOrDefault(player.getUniqueId(), 0L) < 500) return;
        formCooldown.put(player.getUniqueId(), now);

        GUITextManager text = plugin.getGUITextManager();

        // --- Simple Form Handling ---
        if (event.getWindow() instanceof SimpleForm form) {
            if (!(event.getResponse() instanceof SimpleResponse response)) return;
            String title = form.title();
            int buttonId = response.buttonId();

            // Faction Menus
            if (title.equals(text.getTitle("faction_main"))) {
                handleFactionMainMenu(player, buttonId);
            } else if (title.equals(text.getTitle("faction_no_faction"))) {
                handleNoFactionMenu(player, buttonId);
            } else if (title.equals(text.getTitle("faction_members"))) {
                if (buttonId == 0) FactionMenuUI.openInviteForm(player);
                else FactionMenuUI.openKickForm(player);
            } else if (title.equals(text.getTitle("faction_land"))) {
                if (buttonId == 0) plugin.getServer().executeCommand(player, "f claim preview");
                else if (buttonId == 1) plugin.getServer().executeCommand(player, "f claim");
                else plugin.getServer().executeCommand(player, "f unclaim");
            } else if (title.equals(text.getTitle("faction_bank"))) {
                if (buttonId == 0) FactionMenuUI.openDepositForm(player);
                else FactionMenuUI.openWithdrawForm(player);
            }
            // Boss Menu
            else if (title.equals(text.getTitle("boss"))) {
                BossMenuUI.handleMenu(plugin, player, buttonId);
            }
            // Market Menu
            else if (title.equals(text.getTitle("market"))) {
                MarketMenuUI.handleMenu(plugin, player, buttonId);
            }
            // Quest Menu
            else if (title.equals(text.getTitle("quest"))) {
                QuestMenuUI.handleMenu(plugin, player, buttonId);
            }
            // Vendor Menus
            else if (title.equals("Select a Vendor")) {
                List<String> vendorIds = new ArrayList<>(plugin.getVendorManager().getVendors().keySet());
                String vendorId = vendorIds.get(buttonId);
                playerVendorInteraction.put(player.getUniqueId(), vendorId);
                VendorMenuUI.openMainMenu(plugin, player, vendorId);
            } else {
                // Check if it's a specific vendor's main menu
                for (Vendor vendor : plugin.getVendorManager().getVendors().values()) {
                    if (title.equals(vendor.getName())) {
                        playerVendorInteraction.put(player.getUniqueId(), vendor.getId());
                        VendorMenuUI.handleMainMenu(plugin, player, vendor.getId(), buttonId);
                        break;
                    }
                }
            }
        }

        // --- Custom Form Handling ---
        if (event.getResponse() instanceof CustomResponse response) {
            plugin.getLogger().info("Form response received: " + event.getWindow().title());
            String title = event.getWindow().title();

            // Faction Forms
            if (title.equals(text.get("gui.form_create_faction.title", "Create Faction"))) {
                plugin.getServer().executeCommand(player, "f create " + response.getInputResponse(0));
            } else if (title.equals(text.get("gui.form_invite_player.title", "Invite Player"))) {
                plugin.getServer().executeCommand(player, "f invite " + response.getInputResponse(0));
            } else if (title.equals(text.get("gui.form_kick_player.title", "Kick Player"))) {
                String memberToKick = response.getDropdownResponse(0).elementText();
                plugin.getServer().executeCommand(player, "f kick " + memberToKick);
            } else if (title.equals(text.get("gui.form_accept_invite.title", "Accept Invite"))) {
                String factionToJoin = response.getDropdownResponse(0).elementText();
                plugin.getServer().executeCommand(player, "f join " + factionToJoin);
            } else if (title.equals(text.get("gui.form_deposit.title", "Deposit to Bank"))) {
                float amount = response.getSliderResponse(0);
                plugin.getServer().executeCommand(player, "f deposit " + (int)amount);
            } else if (title.equals(text.get("gui.form_withdraw.title", "Withdraw from Bank"))) {
                float amount = response.getSliderResponse(0);
                plugin.getServer().executeCommand(player, "f withdraw " + (int)amount);
            }

            // Vendor Forms
            String vendorId = playerVendorInteraction.get(player.getUniqueId());
            if (vendorId != null) {
                Vendor vendor = plugin.getVendorManager().getVendor(vendorId);
                if (vendor != null) {
                    if (title.equals("Buy from " + vendor.getName())) {
                        int itemIndex = response.getDropdownResponse(0).elementId();
                        int amount = (int) response.getSliderResponse(1);
                        VendorMenuUI.handleBuyMenu(plugin, player, vendorId, itemIndex, amount);
                    } else if (title.equals("Sell to " + vendor.getName())) {
                        int itemIndex = response.getDropdownResponse(0).elementId();
                        int amount = (int) response.getSliderResponse(1);
                        VendorMenuUI.handleSellMenu(plugin, player, vendorId, itemIndex, amount);
                    }
                }
            }
        }
    }

    private void handleFactionMainMenu(Player player, int buttonId) {
        switch (buttonId) {
            case 0: plugin.getServer().executeCommand(player, "f info"); break;
            case 1: FactionMenuUI.openMemberMenu(player); break;
            case 2: FactionMenuUI.openLandMenu(player); break;
            case 3: FactionMenuUI.openBankMenu(player); break;
            case 4: plugin.getServer().executeCommand(player, "f home"); break;
            case 5: FactionMenuUI.openLeaveConfirm(player); break;
            case 6: FactionMenuUI.openDisbandConfirm(player); break;
        }
    }

    private void handleNoFactionMenu(Player player, int buttonId) {
        switch (buttonId) {
            case 0: FactionMenuUI.openCreateFactionForm(player); break;
            case 1: plugin.getServer().executeCommand(player, "f list"); break;
            case 2: FactionMenuUI.openAcceptInviteForm(player); break;
        }
    }
}
