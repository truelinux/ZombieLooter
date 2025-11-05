package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.response.SimpleResponse;
import cn.nukkit.form.window.SimpleForm;
import cn.nukkit.level.Sound;
import com.zombielooter.ZombieLooterX;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles responses for all GUI menus (Quest, Market, Faction, Boss)
 * Now includes cooldown prevention to stop double-execution.
 */
public class GUIFormListener implements Listener {

    private final ZombieLooterX plugin;
    private final Map<UUID, Long> formCooldown = new HashMap<>();

    public GUIFormListener(ZombieLooterX plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFormRespond(PlayerFormRespondedEvent event) {
        if (!(event.getWindow() instanceof SimpleForm form)) return;
        if (!(event.getResponse() instanceof SimpleResponse response)) return;

        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;

        // Prevent double triggers (e.g. rapid form resend)
        long now = System.currentTimeMillis();
        long last = formCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 500) return; // 0.5 second cooldown
        formCooldown.put(player.getUniqueId(), now);

        int index = response.buttonId();
        String title = form.title();

        if (title.contains("Quest")) {
            QuestMenuUI.handleMenu(plugin, player, index);
        } else if (title.contains("Market")) {
            MarketMenuUI.handleMenu(plugin, player, index);
        } else if (title.contains("Faction")) {
            FactionMenuUI.handleMenu(plugin, player, title, index);
        } else if (title.contains("Boss")) {
            BossMenuUI.handleMenu(plugin, player, index);
        }
    }
}
