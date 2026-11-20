package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BedrockStatusCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public BedrockStatusCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");

        boolean geyserActive = geyser != null && geyser.isEnabled();
        boolean floodgateActive = floodgate != null && floodgate.isEnabled();

        int statusCode;
        String statusText;
        String color;

        if (!geyserActive) {
            statusCode = 503;
            statusText = "OFFLINE (Bedrock service is down)";
            color = "§c";
        } else {
            // Check if there is an outdated indicator
            String geyserVer = geyser.getDescription().getVersion();
            // Code 100 reserved for Outdated, Code 200 for Good to join
            if (geyserVer.contains("OUTDATED") || geyserVer.contains("deprecated")) {
                statusCode = 100;
                statusText = "OUTDATED (Bedrock versie is verouderd)";
                color = "§e";
            } else {
                statusCode = 200;
                statusText = "OK (Het spel is goed en spelers kunnen joinen)";
                color = "§a";
            }
        }

        List<String> bedrockPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean isBedrock = (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(p))
                    || p.getName().startsWith(".");
            if (isBedrock) {
                bedrockPlayers.add(p.getName());
            }
        }

        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtil.colorize("§6§l✦ FPIXEL BEDROCK STATUS ✦"));
        sender.sendMessage(ChatUtil.colorize(" §7Status Code: " + color + "§l[" + statusCode + "] " + statusText));
        sender.sendMessage(ChatUtil.colorize(" §7Server Adres: §fmc.fpixel.nl"));
        sender.sendMessage(ChatUtil.colorize(" §7Bedrock Poort: §e19132 §7(RakNet UDP + TCP Signaling)"));
        sender.sendMessage(ChatUtil.colorize(" §7NetherNet WebRTC: §e19133 §7(UDP Gameplay Stream)"));
        sender.sendMessage(ChatUtil.colorize(" §7Geyser: " + (geyserActive ? "§aActief (" + geyser.getDescription().getVersion() + ")" : "§cInactief")));
        sender.sendMessage(ChatUtil.colorize(" §7Floodgate: " + (floodgateActive ? "§aActief" : "§cInactief")));
        sender.sendMessage(ChatUtil.colorize(" §7Online Bedrock Spelers: §f" + bedrockPlayers.size() + (bedrockPlayers.isEmpty() ? "" : " §8(" + String.join(", ", bedrockPlayers) + "§8)")));
        sender.sendMessage(ChatUtil.colorize(" §8Status codes: §e100=Outdated §7| §a200=Goed om te joinen §7| §c503=Down"));
        sender.sendMessage(ChatUtil.colorize("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        if (sender instanceof Player p) {
            ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, statusCode == 200 ? 1.5f : 0.8f);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
