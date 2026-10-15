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
        if (!sender.hasPermission("fpixel.admin")) {
            ChatUtil.sendMessage(sender, "§cYou do not have permission to run backups.");
            return true;
        }

        if (isBackupRunning) {
            ChatUtil.sendMessage(sender, "§cA survival world backup is already in progress. Please wait...");
            return true;
        }

        isBackupRunning = true;
        ChatUtil.sendMessage(sender, "§6[Backup] §eStarting survival world backup directly to GitHub branch §b'backups_survival'§e...");
        ChatUtil.sendMessage(sender, "§7No persistent backup files will be stored locally.");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Find script
                File script = new File("/home/jayv/fpixel/scripts/backup_survival_github.sh");
                if (!script.exists()) {
                    File localScript = new File(plugin.getServer().getWorldContainer().getParentFile(), "scripts/backup_survival_github.sh");
                    if (localScript.exists()) {
                        script = localScript;
                    }
                }

                if (!script.exists()) {
                    isBackupRunning = false;
                    ChatUtil.sendMessage(sender, "§c[Backup] Error: backup_survival_github.sh not found!");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder("/bin/bash", script.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        plugin.getLogger().info("[SurvivalBackup] " + line);
                    }
                }

                int exitCode = process.waitFor();
                isBackupRunning = false;

                if (exitCode == 0) {
                    ChatUtil.sendMessage(sender, "§a§l[Backup Success] §fSurvival world backup was successfully pushed to GitHub (§bbackups_survival§f)!");
                    ChatUtil.sendMessage(sender, "§aAll temporary files were purged locally.");
                } else {
                    ChatUtil.sendMessage(sender, "§c§l[Backup Warning] §fBackup script exited with code " + exitCode + ". Check server logs.");
                }
            } catch (Exception e) {
                isBackupRunning = false;
                plugin.getLogger().severe("Survival backup failed: " + e.getMessage());
                ChatUtil.sendMessage(sender, "§c§l[Backup Error] §fException during backup: " + e.getMessage());
            }
        });

        return true;
    }
}
