package com.zombielooter.gui;

import cn.nukkit.Player;
import cn.nukkit.level.Sound;
import cn.nukkit.utils.TextFormat;

/**
 * Centralized feedback helper for toasts, popups, and action bars.
 * Use this instead of raw sendMessage() where possible, to
 * keep the experience immersive and clean.
 */
public final class FeedbackUtil {

    private FeedbackUtil() {
    }

    public static void toast(Player player, String title, String subtitle) {
        if (player == null || !player.isOnline()) return;
        player.sendTitle(color(title), color(subtitle), 5, 30, 5);
        try {
            player.getLevel().addSound(player, Sound.RANDOM_LEVELUP);
        } catch (Exception ignored) {}
    }

    public static void toastSuccess(Player player, String msg) {
        toast(player, "&a&oSuccess", "&f" + msg);
    }

    public static void toastError(Player player, String msg) {
        toast(player, "&c&o- Error", "&f" + msg);
    }

    public static void popup(Player player, String msg) {
        if (player == null || !player.isOnline()) return;
        player.sendPopup(color(msg));
    }

    public static void actionBar(Player player, String msg) {
        if (player == null || !player.isOnline()) return;
        player.sendActionBar(color(msg));
    }

    public static void info(Player player, String msg) {
        if (player == null || !player.isOnline()) return;
        player.sendMessage(TextFormat.colorize('&', color("&7[&bZLX&7] &f" + msg)));
    }

    private static String color(String s) {
        return TextFormat.colorize('&', s);
    }
}
