package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class ServerUpdateCommand implements CommandExecutor {
    private final FPixelPlugin plugin;
    private static volatile boolean isUpdateRunning = false;

    public ServerUpdateCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("fpixel.owner") && !sender.isOp()) {
            ChatUtil.sendMessage(sender, "§cYou do not have permission to update or restart the server (Owner only).");
            return true;
        }

        if (isUpdateRunning) {
            ChatUtil.sendMessage(sender, "§cA server update is already in progress. Please wait...");
            return true;
        }

        isUpdateRunning = true;
        ChatUtil.sendMessage(sender, "§6§l[Server Update] §eStarting whole-server update (backup, git pull, plugin compile, restart)...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File script = new File("/home/jayv/fpixel/scripts/update_server.sh");
                if (!script.exists()) {
                    File localScript = new File(plugin.getServer().getWorldContainer().getParentFile(), "scripts/update_server.sh");
                    if (localScript.exists()) {
                        script = localScript;
                    }
                }

                if (!script.exists()) {
                    isUpdateRunning = false;
                    ChatUtil.sendMessage(sender, "§c[Server Update] Error: update_server.sh not found!");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        plugin.getLogger().info("[ServerUpdate] " + line);
                    }
                }

                int exitCode = process.waitFor();
                isUpdateRunning = false;

                if (exitCode == 0) {
                    ChatUtil.sendMessage(sender, "§a§l[Update Success] §fWhole-server update completed successfully!");
                } else {
                    ChatUtil.sendMessage(sender, "§c§l[Update Warning] §fUpdate script exited with code " + exitCode + ". Check console logs.");
                }
            } catch (Exception e) {
                isUpdateRunning = false;
                plugin.getLogger().severe("Server update failed: " + e.getMessage());
                ChatUtil.sendMessage(sender, "§c§l[Update Error] §fException: " + e.getMessage());
            }
        });

        return true;
    }
}
