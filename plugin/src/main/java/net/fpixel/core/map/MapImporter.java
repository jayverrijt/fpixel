package net.fpixel.core.map;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bridge.BridgeGame;
import net.fpixel.games.murdermystery.MurderMysteryGame;
import net.fpixel.games.parkour.ParkourGame;
import net.fpixel.games.prophunt.PropHuntGame;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MapImporter {
    private final FPixelPlugin plugin;
    private final File importWorldsDir;
    private final File importMapsDir;
    private final List<String> importedMaps = new ArrayList<>();

    public MapImporter(FPixelPlugin plugin) {
        this.plugin = plugin;
        File container = plugin.getServer().getWorldContainer();
        File rootDir = container.getParentFile() != null ? container.getParentFile() : container;
        this.importWorldsDir = new File(rootDir, "import_worlds");
        this.importMapsDir = new File(rootDir, "import_maps");

        if (!importWorldsDir.exists()) importWorldsDir.mkdirs();
        if (!importMapsDir.exists()) importMapsDir.mkdirs();

        // Check and auto-scan on startup
        Bukkit.getScheduler().runTaskLater(plugin, this::autoScanAndImport, 60L);
    }

    public List<File> getAvailableZipFiles() {
        List<File> list = new ArrayList<>();
        File[] dirs = {importWorldsDir, importMapsDir};
        for (File dir : dirs) {
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip") && !name.toLowerCase().endsWith(".imported"));
                if (files != null) {
                    list.addAll(Arrays.asList(files));
                }
            }
        }
        return list;
    }

    public List<String> getImportedMaps() {
        return Collections.unmodifiableList(importedMaps);
    }

    public void autoScanAndImport() {
        List<File> zips = getAvailableZipFiles();
        if (zips.isEmpty()) return;

        plugin.getLogger().info("Found " + zips.size() + " map archive(s) ready to import.");
        for (File zip : zips) {
            String name = zip.getName().toLowerCase();
            if (name.contains("hub") || name.contains("lobby")) {
                importHub(Bukkit.getConsoleSender(), zip.getName());
            } else if (name.startsWith("bw_") || name.contains("bedwars")) {
                String mapName = sanitizeName(zip.getName());
                importMinigameMap(Bukkit.getConsoleSender(), "BEDWARS", mapName, zip.getName());
            } else if (name.startsWith("sw_") || name.contains("skywars")) {
                String mapName = sanitizeName(zip.getName());
                importMinigameMap(Bukkit.getConsoleSender(), "SKYWARS", mapName, zip.getName());
            }
        }
    }

    public boolean importHub(org.bukkit.command.CommandSender sender, String targetFileName) {
        File zipFile = findZipFile(targetFileName);
        if (zipFile == null) {
            ChatUtil.sendMessage(sender, "§cCould not find file '" + targetFileName + "' in import_worlds/ or import_maps/!");
            return false;
        }

        ChatUtil.sendMessage(sender, "§6[Hub Import] §eUnpacking custom Hub map from §b" + zipFile.getName() + "§e...");
        File worldContainer = plugin.getServer().getWorldContainer();
        File destDir = new File(worldContainer, "custom_hub");

        try {
            extractZip(zipFile, destDir);
            cleanLockFiles(destDir);

            // Load world
            WorldCreator wc = new WorldCreator("custom_hub");
            wc.environment(World.Environment.NORMAL);
            World customHub = wc.createWorld();

            if (customHub != null) {
                customHub.setAutoSave(true);
                customHub.setDifficulty(Difficulty.PEACEFUL);
                customHub.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                customHub.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                customHub.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

                Location spawn = customHub.getSpawnLocation();
                if (spawn.getY() <= 0) spawn.setY(customHub.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ()) + 1);

                // Update lobby manager spawn location
                plugin.getLobbyManager().setSpawn(spawn);

                // Teleport all players currently in lobby to new Hub
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getGameManager().getPlayerArena(p) == null &&
                            (plugin.getSurvivalManager() == null || !plugin.getSurvivalManager().isPlayerInSurvival(p))) {
                        plugin.getLobbyManager().teleportToLobby(p);
                    }
                }

                importedMaps.add("Hub: " + zipFile.getName());
                renameToImported(zipFile);
                ChatUtil.sendMessage(sender, "§a§l[Hub Success] §fNew custom Hub successfully activated! Spawn set to " + spawn.getBlockX() + ", " + spawn.getBlockY() + ", " + spawn.getBlockZ());
                return true;
            } else {
                ChatUtil.sendMessage(sender, "§c[Hub Import] Failed to initialize Minecraft world instance for custom_hub.");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Hub import failed: " + e.getMessage());
            ChatUtil.sendMessage(sender, "§c[Hub Import Error] " + e.getMessage());
            return false;
        }
    }

    public boolean importMinigameMap(org.bukkit.command.CommandSender sender, String gameType, String displayName, String targetFileName) {
        File zipFile = findZipFile(targetFileName);
        if (zipFile == null) {
            ChatUtil.sendMessage(sender, "§cCould not find file '" + targetFileName + "' in import_worlds/ or import_maps/!");
            return false;
        }

        String worldId = "map_" + gameType.toLowerCase() + "_" + System.currentTimeMillis() % 10000;
        File worldContainer = plugin.getServer().getWorldContainer();
        File destDir = new File(worldContainer, worldId);

        ChatUtil.sendMessage(sender, "§6[Map Import] §eUnpacking §b" + displayName + " §efor §a" + gameType + "§e...");

        try {
            extractZip(zipFile, destDir);
            cleanLockFiles(destDir);

            WorldCreator wc = new WorldCreator(worldId);
            wc.environment(World.Environment.NORMAL);
            World mapWorld = wc.createWorld();

            if (mapWorld == null) {
                ChatUtil.sendMessage(sender, "§cFailed to create world for imported map.");
                return false;
            }

            mapWorld.setAutoSave(false);
            mapWorld.setKeepSpawnInMemory(false);
            mapWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            mapWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            mapWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            mapWorld.setTime(6000L);

            Location center = mapWorld.getSpawnLocation();
            if (center.getY() <= 10) {
                center.setY(100);
            }

            Location waitingLobby = center.clone().add(0, 35, 0);
            MapBuilder.buildWaitingLobby(mapWorld, waitingLobby, Material.SMOOTH_STONE, Material.DEEPSLATE_BRICK_WALL, Material.LIGHT_BLUE_STAINED_GLASS);

            String arenaId = "custom-" + gameType.toLowerCase() + "-" + (importedMaps.size() + 1);

            switch (gameType.toUpperCase()) {
                case "BEDWARS" -> {
                    BedWarsGame bw = new BedWarsGame(plugin, arenaId, displayName, 2, 8);
                    bw.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    bw.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    plugin.getGameManager().registerArena(bw);
                }
                case "SKYWARS" -> {
                    SkyWarsGame sw = new SkyWarsGame(plugin, arenaId, displayName, 2, 8, 40);
                    sw.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    sw.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    plugin.getGameManager().registerArena(sw);
                }
                case "BRIDGE" -> {
                    BridgeGame bg = new BridgeGame(plugin, arenaId, displayName, 40);
                    bg.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    bg.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    plugin.getGameManager().registerArena(bg);
                }
                case "SUMO" -> {
                    SumoGame sg = new SumoGame(plugin, arenaId, displayName, 50);
                    sg.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    sg.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    sg.setSpawn1(center.clone().add(-4, 1, 0));
                    sg.setSpawn2(center.clone().add(4, 1, 0));
                    plugin.getGameManager().registerArena(sg);
                }
                case "TNTRUN" -> {
                    TNTRunGame tg = new TNTRunGame(plugin, arenaId, displayName, 2, 8, 30);
                    tg.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    tg.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    tg.setArenaSpawn(center.clone().add(0, 1, 0));
                    plugin.getGameManager().registerArena(tg);
                }
                case "MURDER_MYSTERY", "MM" -> {
                    MurderMysteryGame mm = new MurderMysteryGame(plugin, arenaId, displayName);
                    mm.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    mm.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    mm.addSpawnPoint(center.clone().add(0, 1, 0));
                    plugin.getGameManager().registerArena(mm);
                }
                case "PROP_HUNT", "PH" -> {
                    PropHuntGame ph = new PropHuntGame(plugin, arenaId, displayName);
                    ph.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    ph.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    ph.setHunterSpawn(waitingLobby.clone().add(0, 1, 0));
                    ph.addPropSpawn(center.clone().add(0, 1, 0));
                    plugin.getGameManager().registerArena(ph);
                }
                default -> {
                    ParkourGame pg = new ParkourGame(plugin, arenaId, displayName, 1, 8, 40);
                    pg.setLobbySpawn(waitingLobby.clone().add(0, 1, 0));
                    pg.setSpectatorSpawn(waitingLobby.clone().add(0, 5, 0));
                    pg.setStartLocation(center.clone().add(0, 1, 0));
                    plugin.getGameManager().registerArena(pg);
                }
            }

            importedMaps.add(gameType.toUpperCase() + ": " + displayName + " (" + arenaId + ")");
            renameToImported(zipFile);

            ChatUtil.sendMessage(sender, "§a§l[Map Success] §fMap §e" + displayName + " §fregistered under §b" + arenaId + " §f(" + gameType + ")!");
            ChatUtil.sendMessage(sender, "§7Players can join using §e/join " + gameType.toLowerCase() + " §7or select it in §e/maps " + gameType.toLowerCase());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Map import failed: " + e.getMessage());
            ChatUtil.sendMessage(sender, "§c[Map Import Error] " + e.getMessage());
            return false;
        }
    }

    private File findZipFile(String name) {
        String clean = name.toLowerCase();
        if (!clean.endsWith(".zip")) clean += ".zip";

        File f1 = new File(importWorldsDir, clean);
        if (f1.exists()) return f1;
        File f2 = new File(importMapsDir, clean);
        if (f2.exists()) return f2;

        // Partial match
        for (File f : getAvailableZipFiles()) {
            if (f.getName().toLowerCase().contains(name.toLowerCase())) {
                return f;
            }
        }
        return null;
    }

    private void extractZip(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) destDir.mkdirs();

        // 1. Detect single root directory inside archive
        String rootDirPrefix = null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.contains("level.dat") || name.contains("region/")) {
                    int idx = name.indexOf('/');
                    if (idx > 0 && !name.startsWith("region/")) {
                        rootDirPrefix = name.substring(0, idx + 1);
                    }
                    break;
                }
            }
        }

        // 2. Unpack stripping outer folder
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (rootDirPrefix != null && name.startsWith(rootDirPrefix)) {
                    name = name.substring(rootDirPrefix.length());
                }
                if (name.isEmpty() || name.contains("__MACOSX") || name.endsWith(".DS_Store")) {
                    zis.closeEntry();
                    continue;
                }

                File newFile = new File(destDir, name);
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

    private void cleanLockFiles(File dir) {
        File sessionLock = new File(dir, "session.lock");
        if (sessionLock.exists()) sessionLock.delete();
        File uidDat = new File(dir, "uid.dat");
        if (uidDat.exists()) uidDat.delete();
    }

    private void renameToImported(File zipFile) {
        try {
            File importedFile = new File(zipFile.getParentFile(), zipFile.getName() + ".imported");
            Files.move(zipFile.toPath(), importedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable ignored) {}
    }

    private String sanitizeName(String filename) {
        String base = filename.replace(".zip", "").replace("_", " ").replace("-", " ");
        String[] parts = base.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
