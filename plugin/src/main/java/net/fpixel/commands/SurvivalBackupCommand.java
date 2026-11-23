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

public class SurvivalBackupCommand implements CommandExecutor {
    private final FPixelPlugin plugin;
    private static volatile boolean isBackupRunning = false;

    public SurvivalBackupCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("fpixel.owner") && !sender.isOp()) {
            ChatUtil.sendMessage(sender, "§cYou do not have permission to run survival backups (Owner only).");
            return true;
        }

        if (isBackupRunning) {
            ChatUtil.sendMessage(sender, "§cA survival world backup is already in progress. Please wait...");
            return true;
        }

        isBackupRunning = true;
        ChatUtil.sendMessage(sender, "§6[Backup] §eStarting daily whole-repository backup directly to GitHub branch §b'daily_backups'§e...");
        ChatUtil.sendMessage(sender, "§7No persistent backup files will be stored locally.");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Find script
                File script = new File("/home/jayv/fpixel/scripts/daily_backup.sh");
                if (!script.exists()) {
                    File localScript = new File(plugin.getServer().getWorldContainer().getParentFile(), "scripts/daily_backup.sh");
                    if (localScript.exists()) {
                        script = localScript;
                    } else {
                        script = new File("/home/jayv/fpixel/scripts/backup_survival_github.sh");
                    }
                }

                if (!script.exists()) {
                    isBackupRunning = false;
                    ChatUtil.sendMessage(sender, "§c[Backup] Error: daily_backup.sh not found!");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        plugin.getLogger().info("[DailyBackup] " + line);
                    }
                }

                int exitCode = process.waitFor();
                isBackupRunning = false;

                if (exitCode == 0) {
                    ChatUtil.sendMessage(sender, "§a§l[Backup Success] §fDaily whole-repository backup was successfully pushed to GitHub (§bdaily_backups§f)!");
                    ChatUtil.sendMessage(sender, "§aAll temporary files were purged locally.");
                } else {
                    ChatUtil.sendMessage(sender, "§c§l[Backup Warning] §fBackup script exited with code " + exitCode + ". Check server logs.");
                }
            } catch (Exception e) {
                isBackupRunning = false;
                plugin.getLogger().severe("Daily backup failed: " + e.getMessage());
                ChatUtil.sendMessage(sender, "§c§l[Backup Error] §fException during backup: " + e.getMessage());
            }
        });

        return true;
    }
}
