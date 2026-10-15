package net.fpixel.core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

public class ChatUtil {
    public static final String PREFIX = "§6§lFPIXEL §8» §f";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static Component component(String text) {
        if (text == null) return Component.empty();
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (Exception ignored) {}
        }
        return LEGACY_SERIALIZER.deserialize(colorize(text));
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(component(PREFIX + message));
    }

    public static void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(component(PREFIX + message));
    }

    public static void sendRawMessage(Player player, String message) {
        player.sendMessage(component(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(component(message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Title t = Title.title(
                component(title),
                component(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeInTicks * 50L),
                        Duration.ofMillis(stayTicks * 50L),
                        Duration.ofMillis(fadeOutTicks * 50L)
                )
        );
        player.showTitle(t);
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void broadcast(String message) {
        Component c = component(PREFIX + message);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(c);
        }
    }
}
