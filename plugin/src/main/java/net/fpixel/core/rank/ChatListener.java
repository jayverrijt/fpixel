package net.fpixel.core.rank;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final FPixelPlugin plugin;

    public ChatListener(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Rank rank = plugin.getRankManager().getRank(player);

        String messageText = PlainTextComponentSerializer.plainText().serialize(event.message());
        String formatted = rank.getPrefix() + rank.getColor() + player.getName() + "§8: §f" + messageText;

        event.renderer((source, sourceDisplayName, message, viewer) -> ChatUtil.component(formatted));
    }
}
