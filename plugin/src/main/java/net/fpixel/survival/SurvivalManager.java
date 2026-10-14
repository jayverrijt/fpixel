package net.fpixel.survival;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SurvivalManager {
    private final FPixelPlugin plugin;
    private World survivalWorld;

    public SurvivalManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        checkAndExtractZip();
        loadSurvivalWorld();
    }

    private void checkAndExtractZip() {
        File container = plugin.getServer().getWorldContainer();
        File targetDir = new File(container, "survival");

        // If level.dat already exists, the world is already installed
        if (new File(targetDir, "level.dat").exists()) {
            plugin.getLogger().info("Survival SMP world is already extracted and ready in " + targetDir.getAbsolutePath());
            return;
        }

        File importDir = new File(container.getParentFile(), "import_worlds");
        File zipFile = new File(importDir, "survival.zip");

        if (!zipFile.exists()) {
            zipFile = new File(container.getParentFile(), "survival.zip");
        }

        if (zipFile.exists() && zipFile.isFile()) {
            plugin.getLogger().info("Found survival world zip: " + zipFile.getAbsolutePath() + "! Extracting...");
            targetDir.mkdirs();

            try {
                // Find world prefix (containing level.dat)
                String worldPrefix = findWorldPrefix(zipFile);
                plugin.getLogger().info("Detected world prefix in zip: '" + worldPrefix + "'");

                extractWorldFromZip(zipFile, targetDir, worldPrefix);

                // Clean up session lock and uid files
                new File(targetDir, "session.lock").delete();
                new File(targetDir, "uid.dat").delete();

                // Setup compatibility symlinks/aliases
                setupCompatibilityLinks(targetDir);

                // Rename zip to .imported so it's not reprocessed
                File importedMarker = new File(zipFile.getParentFile(), zipFile.getName() + ".imported");
                zipFile.renameTo(importedMarker);
                plugin.getLogger().info("Successfully imported survival.zip into server/survival/!");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to extract survival.zip: " + e.getMessage());
            }
        }
    }

    private String findWorldPrefix(File zipFile) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith("level.dat")) {
                    return name.substring(0, name.lastIndexOf("level.dat"));
                }
                zis.closeEntry();
            }
        }
        return "";
    }

    private void extractWorldFromZip(File zipFile, File destDir, String worldPrefix) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith(worldPrefix)) {
                    zis.closeEntry();
                    continue;
                }
                String relPath = name.substring(worldPrefix.length());
                if (relPath.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                File newFile = new File(destDir, relPath);
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void setupCompatibilityLinks(File targetDir) {
        // dimensions/minecraft/overworld/region -> region
        File dimRegion = new File(targetDir, "dimensions/minecraft/overworld/region");
        File topRegion = new File(targetDir, "region");
        if (dimRegion.exists() && !topRegion.exists()) {
            try {
                Files.createSymbolicLink(topRegion.toPath(), dimRegion.toPath());
            } catch (Exception ignored) {}
        }

        // players/data -> playerdata
        File playersData = new File(targetDir, "players/data");
        File topPlayerdata = new File(targetDir, "playerdata");
        if (playersData.exists() && !topPlayerdata.exists()) {
            try {
                Files.createSymbolicLink(topPlayerdata.toPath(), playersData.toPath());
            } catch (Exception ignored) {}
        }
    }


    private void loadSurvivalWorld() {
        plugin.getLogger().info("Loading Survival SMP world...");
        WorldCreator wc = new WorldCreator("survival");
        wc.environment(World.Environment.NORMAL);

        try {
            this.survivalWorld = wc.createWorld();
            if (survivalWorld != null) {
                survivalWorld.setDifficulty(Difficulty.NORMAL);
                survivalWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                survivalWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
                survivalWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
                survivalWorld.setAutoSave(true);
                plugin.getLogger().info("Survival SMP world is ready at: " + survivalWorld.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load survival world: " + e.getMessage());
        }
    }

    public void sendToSurvival(Player player) {
        if (survivalWorld == null) {
            loadSurvivalWorld();
        }
        if (survivalWorld == null) {
            ChatUtil.sendMessage(player, "§cSurvival world is currently not available!");
            return;
        }

        // Leave any current arena
        Arena arena = plugin.getGameManager().getPlayerArena(player);
        if (arena != null) {
            arena.leave(player);
        }

        // Teleport to survival spawn
        Location spawn = survivalWorld.getSpawnLocation();
        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);

        ChatUtil.sendTitle(player, "§a§lSURVIVAL SMP", "§eBuild, Mine, and Survive!", 10, 50, 15);
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        ChatUtil.sendMessage(player, "§6§m--------------------------------------------------");
        ChatUtil.sendMessage(player, "§a§lSURVIVAL SMP §8» §fWelcome to the Survival World!");
        ChatUtil.sendMessage(player, "§7• Normal vanilla gameplay: building, crafting, mining & mobs.");
        ChatUtil.sendMessage(player, "§7• Use §e/lobby §7or §e/hub §7to return to the main lobby anytime.");
        ChatUtil.sendMessage(player, "§6§m--------------------------------------------------");
    }

    public World getSurvivalWorld() {
        return survivalWorld;
    }
}
