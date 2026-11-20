package net.fpixel.core.watchdog;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FPixel Custom Watchdog Anticheat & Client Integrity System.
 * Maintains an active Trust Score (0 - 100) for every connected player.
 * Minimum required trust score to play is 90/100.
 * Hack clients such as Wurst, Meteor, Aristois, LiquidBounce, etc. are immediately assigned 0 points and kicked.
 */
public class WatchdogManager {

    public static final int MIN_REQUIRED_SCORE = 90;
    public static final int MAX_TRUST_SCORE = 100;

    private final FPixelPlugin plugin;
    private final Map<UUID, Integer> trustScores = new ConcurrentHashMap<>();
    private final Map<UUID, String> clientBrands = new ConcurrentHashMap<>();
    private final List<ViolationRecord> recentViolations = Collections.synchronizedList(new ArrayList<>());
    private final File logFile;

    // Known banned cheat/hack client signatures (case-insensitive)
    private static final Set<String> BANNED_BRANDS = Set.of(
            "wurst", "worst", "meteor", "liquidbounce", "aristois", "sigma",
            "impact", "bleachhack", "inertia", "future", "rusherhack", "lambda",
            "kami", "forgehax", "jex", "ares", "coffee", "matix", "vape",
            "huzuni", "nodus", "xray", "killaura", "cheat", "hack", "autoclicker",
            "wurstclient", "liquid", "meteorclient"
    );

    public record ViolationRecord(String timestamp, String player, String uuid, String brand, int score, String reason) {}

    public WatchdogManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "watchdog_log.json");
    }

    /**
     * Initializes trust score for joining player.
     */
    public void registerPlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        trustScores.putIfAbsent(uuid, MAX_TRUST_SCORE);
    }

    public void unregisterPlayer(Player player) {
        if (player == null) return;
        // Keep score in memory briefly in case of reconnect, but cleanup brand
        clientBrands.remove(player.getUniqueId());
    }

    public int getTrustScore(UUID uuid) {
        return trustScores.getOrDefault(uuid, MAX_TRUST_SCORE);
    }

    public int getTrustScore(Player player) {
        return player == null ? MAX_TRUST_SCORE : getTrustScore(player.getUniqueId());
    }

    public void setTrustScore(UUID uuid, int score) {
        trustScores.put(uuid, Math.max(0, Math.min(MAX_TRUST_SCORE, score)));
    }

    public String getClientBrand(Player player) {
        if (player == null) return "Unknown";
        return clientBrands.getOrDefault(player.getUniqueId(), "Unknown");
    }

    /**
     * Analyzes and validates the client brand reported by the client or handshake.
     * Hack clients receive 0 points.
     */
    public void processClientBrand(Player player, String brand) {
        if (player == null) return;
        String cleanBrand = (brand != null && !brand.isBlank()) ? brand.trim() : "Unknown";
        clientBrands.put(player.getUniqueId(), cleanBrand);

        int score = evaluateBrand(player, cleanBrand);
        trustScores.put(player.getUniqueId(), score);

        if (score < MIN_REQUIRED_SCORE) {
            String reason = "Verboden hack-client gedetecteerd (" + cleanBrand + ")";
            kickForViolation(player, score, reason);
        } else {
            plugin.getLogger().info("[Watchdog] Player " + player.getName() + " passed client inspection (Brand: " + cleanBrand + ", Score: " + score + "/100)");
        }
    }

    /**
     * Evaluates trust score based on client brand.
     */
    public int evaluateBrand(Player player, String brand) {
        if (brand == null || brand.isBlank()) {
            // Missing brand handshake
            return 80;
        }

        String lower = brand.toLowerCase();
        for (String banned : BANNED_BRANDS) {
            if (lower.contains(banned)) {
                return 0; // Immediate 0 points for hack clients!
            }
        }

        // Bedrock Floodgate players are verified through official Bedrock protocol
        if (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(player)) {
            return 100;
        }

        return 100;
    }

    /**
     * Deducts points from a player's trust score when suspicious behavior is flagged.
     * If trust score falls below 90, Watchdog automatically kicks the player.
     */
    public void penalize(Player player, int points, String reason) {
        if (player == null || !player.isOnline()) return;

        // Exempt server owner and staff from heuristic movement penalties
        if (player.hasPermission("fpixel.owner") || (plugin.getRankManager() != null && plugin.getRankManager().getRank(player).isOwner())) {
            return;
        }

        // Exempt Bedrock Edition players from automated heuristic penalties to prevent false-positive kicks (e.g. error Terracotta)
        boolean isBedrock = (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(player))
                || player.getName().startsWith(".");
        if (isBedrock) {
            plugin.getLogger().info("[Watchdog] Heuristic warning suppressed for Bedrock player " + player.getName() + ": " + reason);
            trustScores.put(player.getUniqueId(), MAX_TRUST_SCORE);
            return;
        }

        // Exempt staff in creative or godmode
        if (player.hasPermission("fpixel.admin") && player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        int current = getTrustScore(player);
        int updated = Math.max(0, current - points);
        trustScores.put(player.getUniqueId(), updated);

        plugin.getLogger().warning("[Watchdog] Flagged " + player.getName() + ": " + reason + " (-" + points + " pts -> " + updated + "/100)");

        if (updated < MIN_REQUIRED_SCORE) {
            kickForViolation(player, updated, reason);
        } else {
            // Send warning to player
            ChatUtil.sendMessage(player, "§c§l[Watchdog] §eWaarschuwing: Ongebruikelijke activiteit gedetecteerd (§c" + reason + "§e). Trust score: §f" + updated + "§7/100 (Min. 90 vereist)");
        }
    }

    /**
     * Kicks a player for having an insufficient trust score (< 90).
     */
    public void kickForViolation(Player player, int score, String reason) {
        if (player == null || !player.isOnline()) return;

        // Exempt server owner
        if (player.hasPermission("fpixel.owner") || (plugin.getRankManager() != null && plugin.getRankManager().getRank(player).isOwner())) {
            setTrustScore(player.getUniqueId(), MAX_TRUST_SCORE);
            return;
        }

        String brand = getClientBrand(player);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        ViolationRecord record = new ViolationRecord(timestamp, player.getName(), player.getUniqueId().toString(), brand, score, reason);
        recentViolations.add(0, record);
        if (recentViolations.size() > 50) {
            recentViolations.remove(recentViolations.size() - 1);
        }
        saveViolationsLog();

        // Alert online staff
        String alert = "§c§l[Watchdog] §4OVERTREDING: §f" + player.getName() + " §7(Client: §c" + brand + "§7) heeft score §c" + score + "/100 §7(Reden: §e" + reason + "§7) §c-> GEKICKT!";
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("fpixel.mod") || online.hasPermission("fpixel.admin") || plugin.getRankManager().getRank(online).isStaff()) {
                ChatUtil.sendMessage(online, alert);
            }
        }

        // Kick banner
        String kickScreen = "§c§lWATCHDOG SECURITY\n" +
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "§4Toegang Geweigerd: Onveilige Client of Modificaties!\n\n" +
                "§7Jouw Trust Score: §c" + score + "§7/§a100 §8(Minimaal 90/100 vereist)\n" +
                "§7Gedetecteerd: §e" + reason + "\n" +
                "§7Client Handshake: §f" + brand + "\n\n" +
                "§cFPixel tolereert geen hackclients of oneerlijke voordelen.\n" +
                "§7Verbind opnieuw met een schone Vanilla, Fabric, Lunar of Badlion client.\n\n" +
                "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.kick(Component.text(kickScreen));
            }
        });
    }

    public List<ViolationRecord> getRecentViolations() {
        return new ArrayList<>(recentViolations);
    }

    private void saveViolationsLog() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            try (FileWriter writer = new FileWriter(logFile, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder("[\n");
                for (int i = 0; i < recentViolations.size(); i++) {
                    ViolationRecord r = recentViolations.get(i);
                    sb.append("  {\n")
                            .append("    \"timestamp\": \"").append(r.timestamp()).append("\",\n")
                            .append("    \"player\": \"").append(r.player()).append("\",\n")
                            .append("    \"uuid\": \"").append(r.uuid()).append("\",\n")
                            .append("    \"brand\": \"").append(r.brand().replace("\"", "\\\"")).append("\",\n")
                            .append("    \"score\": ").append(r.score()).append(",\n")
                            .append("    \"reason\": \"").append(r.reason().replace("\"", "\\\"")).append("\"\n")
                            .append("  }").append(i < recentViolations.size() - 1 ? "," : "").append("\n");
                }
                sb.append("]");
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save watchdog_log.json: " + e.getMessage());
        }
    }
}
