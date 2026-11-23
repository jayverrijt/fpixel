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

public class ServerBackupCommand implements CommandExecutor {
    private final FPixelPlugin plugin;
    private static volatile boolean isBackupRunning = false;

    public ServerBackupCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("fpixel.owner") && !sender.isOp()) {
            ChatUtil.sendMessage(sender, "§cYou do not have permission to run server backups (Owner only).");
            return true;
        }

        if (isBackupRunning) {
            ChatUtil.sendMessage(sender, "§cA server backup is already in progress. Please wait...");
            return true;
        }

        isBackupRunning = true;
        ChatUtil.sendMessage(sender, "§6[Backup] §eStarting full server backup (worlds, configs, plugins)...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File script = new File("/home/jayv/fpixel/scripts/backup.sh");
                if (!script.exists()) {
                    File localScript = new File(plugin.getServer().getWorldContainer().getParentFile(), "scripts/backup.sh");
                    if (localScript.exists()) {
                        script = localScript;
                    }
                }

                if (!script.exists()) {
                    isBackupRunning = false;
                    ChatUtil.sendMessage(sender, "§c[Backup] Error: backup.sh not found!");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        plugin.getLogger().info("[ServerBackup] " + line);
                    }
                }

                int exitCode = process.waitFor();
                isBackupRunning = false;

                if (exitCode == 0) {
                    ChatUtil.sendMessage(sender, "§a§l[Backup Success] §fWhole-server backup created successfully in /backups!");
                } else {
                    ChatUtil.sendMessage(sender, "§c§l[Backup Warning] §fBackup script exited with code " + exitCode + ".");
                }
            } catch (Exception e) {
                isBackupRunning = false;
                plugin.getLogger().severe("Server backup failed: " + e.getMessage());
                ChatUtil.sendMessage(sender, "§c§l[Backup Error] §fException: " + e.getMessage());
            }
        });

        return true;
    }
}
