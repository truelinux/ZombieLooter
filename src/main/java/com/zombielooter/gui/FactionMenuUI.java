package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.form.element.custom.ElementDropdown;
import cn.nukkit.form.element.custom.ElementInput;
import cn.nukkit.form.element.custom.ElementSlider;
import cn.nukkit.form.window.CustomForm;
import cn.nukkit.form.window.ModalForm;
import cn.nukkit.form.window.SimpleForm;
import com.zombielooter.ZombieLooterX;
import com.zombielooter.factions.Faction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import cn.nukkit.utils.TextFormat;

public final class FactionMenuUI {

    private static ZombieLooterX plugin;

    public static void init(ZombieLooterX p) {
        plugin = p;
    }

    private FactionMenuUI() {}

    public static void openMainMenu(Player player) {
        Faction faction = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        GUITextManager text = plugin.getGUITextManager();
        if (faction != null) {
            SimpleForm form = new SimpleForm(
                text.getTitle("faction_main"),
                text.getDescription("faction_main")
            );
            for (String button : text.getButtons("faction_main")) {
                form.addButton(button);
            }
            form.send(player);
        } else {
            SimpleForm form = new SimpleForm(
                text.getTitle("faction_no_faction"),
                text.getDescription("faction_no_faction")
            );
            for (String button : text.getButtons("faction_no_faction")) {
                form.addButton(button);
            }
            form.send(player);
        }
    }

    // --- Sub Menus ---

    public static void openMemberMenu(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        SimpleForm form = new SimpleForm(text.getTitle("faction_members"));
        form.content(text.getDescription("faction_members"));
        for (String button : text.getButtons("faction_members")) {
            form.addButton(button);
        }
        form.send(player);
    }

    public static void openLandMenu(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        SimpleForm form = new SimpleForm(text.getTitle("faction_land"));
        form.content(text.getDescription("faction_land"));
        for (String button : text.getButtons("faction_land")) {
            form.addButton(button);
        }
        form.send(player);
    }

    public static void openBankMenu(Player player) {
        Faction faction = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        if (faction == null) return;
        GUITextManager text = plugin.getGUITextManager();
        SimpleForm form = new SimpleForm(
            text.getTitle("faction_bank"),
            "&7Current Balance: &e" + String.format("%.2f", faction.getBankBalance()) + " coins"
        );
        for (String button : text.getButtons("faction_bank")) {
            form.addButton(button);
        }
        form.send(player);
    }

    // --- Forms ---

    public static void openCreateFactionForm(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        CustomForm form = new CustomForm(text.getText("form_create_faction.title", "Create Faction"));
        form.addElement(new ElementInput(text.getText("form_create_faction.input_label", "Faction Name")));
        form.send(player);
    }

    public static void openInviteForm(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        CustomForm form = new CustomForm(text.getText("form_invite_player.title", "Invite Player"));
        form.addElement(new ElementInput(text.getText("form_invite_player.input_label", "Player Name")));
        form.send(player);
    }

    public static void openKickForm(Player player) {
        Faction faction = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        if (faction == null) return;
        GUITextManager text = plugin.getGUITextManager();
        
        List<String> memberNames = faction.getMembers().stream()
            .map(uuid -> plugin.getServer().getOfflinePlayer(uuid).getName())
            .filter(name -> !name.equals(player.getName()))
            .collect(Collectors.toList());

        if (memberNames.isEmpty()) {
            player.sendMessage(TextFormat.colorize('&', "&cThere are no other members to kick."));
            return;
        }

        CustomForm form = new CustomForm(text.getText("form_kick_player.title", "Kick Player"));
        form.addElement(new ElementDropdown(text.getText("form_kick_player.dropdown_label", "Select Member"), memberNames));
        form.send(player);
    }

    public static void openAcceptInviteForm(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        List<String> invites = new ArrayList<>();
        for (Faction f : plugin.getFactionManager().getFactions()) {
            if (f.hasInvite(player.getUniqueId())) {
                invites.add(f.getName());
            }
        }

        if (invites.isEmpty()) {
            player.sendMessage(TextFormat.colorize('&', "&cYou have no pending faction invitations."));
            return;
        }

        CustomForm form = new CustomForm(text.getText("form_accept_invite.title", "Accept Invite"));
        form.addElement(new ElementDropdown(text.getText("form_accept_invite.dropdown_label", "Select Faction"), invites));
        form.send(player);
    }

    public static void openDepositForm(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        if (plugin.getEconomyManager().getBalance(player.getUniqueId()) <= 0) {
            player.sendMessage(TextFormat.colorize('&', text.getText("feedback.no_money_to_deposit", "&cYou have no money to deposit.")));
            return;
        }
        CustomForm form = new CustomForm(text.getText("form_deposit.title", "Deposit to Bank"));
        form.addElement(new ElementSlider(text.getText("form_deposit.slider_label", "Amount"), 1, (float) plugin.getEconomyManager().getBalance(player.getUniqueId()), 1, 100));
        form.send(player);
    }

    public static void openWithdrawForm(Player player) {
        Faction faction = plugin.getFactionManager().getFactionByPlayer(player.getUniqueId());
        if (faction == null) return;
        GUITextManager text = plugin.getGUITextManager();
        CustomForm form = new CustomForm(text.getText("form_withdraw.title", "Withdraw from Bank"));
        form.addElement(new ElementSlider(text.getText("form_withdraw.slider_label", "Amount"), 1, (float) faction.getBankBalance(), 1, 100));
        form.send(player);
    }

    // --- Confirmations ---

    public static void openDisbandConfirm(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        new ModalForm(
            text.getText("form_confirm_disband.title", "Confirm Disband"),
            text.getText("form_confirm_disband.content", "Are you sure?")
        ).text(
            text.getText("form_confirm_disband.confirm_button", "Yes"),
            text.getText("form_confirm_disband.cancel_button", "No")
        ).onYes(p -> {
            plugin.getServer().executeCommand(p, "f disband");
        }).send(player);
    }

    public static void openLeaveConfirm(Player player) {
        GUITextManager text = plugin.getGUITextManager();
        new ModalForm(
            text.getText("form_confirm_leave.title", "Confirm Leave"),
            text.getText("form_confirm_leave.content", "Are you sure?")
        ).text(
            text.getText("form_confirm_leave.confirm_button", "Yes"),
            text.getText("form_confirm_leave.cancel_button", "No")
        ).onYes(p -> {
            plugin.getServer().executeCommand(p, "f leave");
        }).send(player);
    }
}
