package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SurvivalCommand implements CommandExecutor {
    private final FPixelPlugin plugin;

    public SurvivalCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can join the Survival SMP.");
            return true;
        }

        plugin.getSurvivalManager().sendToSurvival(player);
        return true;
    }
}
