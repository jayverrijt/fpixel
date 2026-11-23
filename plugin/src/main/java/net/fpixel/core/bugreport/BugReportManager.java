package net.fpixel.core.bugreport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fpixel.FPixelPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BugReportManager {

    private final FPixelPlugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<BugReport> reports = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger nextId = new AtomicInteger(1);

    public static class BugReport {
        private int id;
        private String reporter;
        private String uuid;
        private String world;
        private String coordinates;
        private String gamemode;
        private String minigame;
        private String description;
        private String timestamp;
        private String status; // "OPEN" or "RESOLVED"
        private String resolvedBy;

        public BugReport() {}

        public BugReport(int id, String reporter, String uuid, String world, String coordinates,
                         String gamemode, String minigame, String description, String timestamp) {
            this.id = id;
            this.reporter = reporter;
            this.uuid = uuid;
            this.world = world;
            this.coordinates = coordinates;
            this.gamemode = gamemode;
            this.minigame = minigame;
            this.description = description;
            this.timestamp = timestamp;
            this.status = "OPEN";
            this.resolvedBy = null;
        }

        public int getId() { return id; }
        public String getReporter() { return reporter; }
        public String getUuid() { return uuid; }
        public String getWorld() { return world; }
        public String getCoordinates() { return coordinates; }
        public String getGamemode() { return gamemode; }
        public String getMinigame() { return minigame; }
        public String getDescription() { return description; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getResolvedBy() { return resolvedBy; }

        public void setStatus(String status) { this.status = status; }
        public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    }

    public BugReportManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bug_reports.json");
        load();
    }

    public synchronized BugReport createReport(Player player, String description) {
        Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "Onbekend";
        String coords = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
        String gamemode = player.getGameMode().name();

        String minigame = "Geen (Lobby/Survival)";
        if (plugin.getGameManager() != null) {
            var arena = plugin.getGameManager().getPlayerArena(player);
            if (arena != null) {
                minigame = arena.getDisplayName() + " [" + arena.getId() + "]";
            }
        }

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        int id = nextId.getAndIncrement();

        BugReport report = new BugReport(id, player.getName(), player.getUniqueId().toString(),
                worldName, coords, gamemode, minigame, description, time);

        reports.add(0, report);
        save();
        return report;
    }

    public synchronized boolean resolveReport(int id, String resolverName) {
        for (BugReport report : reports) {
            if (report.getId() == id) {
                report.setStatus("RESOLVED");
                report.setResolvedBy(resolverName);
                save();
                return true;
            }
        }
        return false;
    }

    public synchronized BugReport getReport(int id) {
        for (BugReport report : reports) {
            if (report.getId() == id) {
                return report;
            }
        }
        return null;
    }

    public List<BugReport> getAllReports() {
        return new ArrayList<>(reports);
    }

    public List<BugReport> getOpenReports() {
        List<BugReport> list = new ArrayList<>();
        for (BugReport r : reports) {
            if ("OPEN".equalsIgnoreCase(r.getStatus())) {
                list.add(r);
            }
        }
        return list;
    }

    public synchronized void load() {
        if (!file.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<BugReport>>(){}.getType();
            List<BugReport> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                reports.clear();
                reports.addAll(loaded);
                int maxId = 0;
                for (BugReport r : loaded) {
                    if (r.getId() > maxId) maxId = r.getId();
                }
                nextId.set(maxId + 1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Kon bug_reports.json niet laden: " + e.getMessage());
        }
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(reports, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Kon bug_reports.json niet opslaan: " + e.getMessage());
        }
    }
}
