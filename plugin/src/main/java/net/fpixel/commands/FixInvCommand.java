package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FixInvCommand implements CommandExecutor {
    private final FPixelPlugin plugin;

    public FixInvCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Alleen spelers kunnen dit commando uitvoeren.");
            return true;
        }

        player.closeInventory();
        player.updateInventory();
        ChatUtil.sendMessage(player, "§a§l✔ §aJe inventory, kisten en containers zijn opnieuw gesynchroniseerd!");
        ChatUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        return true;
    }
}
