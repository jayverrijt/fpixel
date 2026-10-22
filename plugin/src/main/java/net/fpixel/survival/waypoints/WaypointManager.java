package net.fpixel.survival.waypoints;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointManager {
    private final FPixelPlugin plugin;
    private final Map<UUID, Map<String, Waypoint>> playerWaypoints = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeTracked = new ConcurrentHashMap<>();
    private final Set<UUID> hudDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final File waypointsDir;

    public WaypointManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.waypointsDir = new File(plugin.getDataFolder(), "survival_data/waypoints");
        if (!waypointsDir.exists()) {
            waypointsDir.mkdirs();
        }
        startMinimapHudTask();
        registerPluginChannels();
    }

    private void registerPluginChannels() {
        try {
            // Register channels for Xaero's Minimap & WorldMap server communication
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "xaerominimap:main");
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "xaeroworldmap:main");
        } catch (Throwable ignored) {}
    }

    public void loadPlayerWaypoints(UUID uuid) {
        File file = new File(waypointsDir, uuid.toString() + ".yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = config.getConfigurationSection("waypoints");
        if (sec == null) return;

        Map<String, Waypoint> map = new HashMap<>();
        for (String key : sec.getKeys(false)) {
            ConfigurationSection wpSec = sec.getConfigurationSection(key);
            if (wpSec == null) continue;

            String name = wpSec.getString("name", key);
            String world = wpSec.getString("world", "survival");
            double x = wpSec.getDouble("x");
            double y = wpSec.getDouble("y");
            double z = wpSec.getDouble("z");
            float yaw = (float) wpSec.getDouble("yaw", 0.0);
            float pitch = (float) wpSec.getDouble("pitch", 0.0);
            String colorName = wpSec.getString("color", "AQUA");
            ChatColor color;
            try {
                color = ChatColor.valueOf(colorName);
            } catch (Exception e) {
                color = ChatColor.AQUA;
            }
            Material icon = Material.matchMaterial(wpSec.getString("icon", "COMPASS"));
            if (icon == null) icon = Material.COMPASS;
            long createdAt = wpSec.getLong("created_at", System.currentTimeMillis());

            Waypoint wp = new Waypoint(name, world, x, y, z, yaw, pitch, color, icon, createdAt);
            map.put(name.toLowerCase(), wp);
        }
        playerWaypoints.put(uuid, map);
    }

    public void savePlayerWaypoints(UUID uuid) {
        Map<String, Waypoint> map = playerWaypoints.get(uuid);
        File file = new File(waypointsDir, uuid.toString() + ".yml");
        if (map == null || map.isEmpty()) {
            if (file.exists()) file.delete();
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection sec = config.createSection("waypoints");
        for (Waypoint wp : map.values()) {
            ConfigurationSection wpSec = sec.createSection(wp.getName().toLowerCase());
            wpSec.set("name", wp.getName());
            wpSec.set("world", wp.getWorldName());
            wpSec.set("x", wp.getX());
            wpSec.set("y", wp.getY());
            wpSec.set("z", wp.getZ());
            wpSec.set("yaw", wp.getYaw());
            wpSec.set("pitch", wp.getPitch());
            wpSec.set("color", wp.getColor().name());
            wpSec.set("icon", wp.getIcon().name());
            wpSec.set("created_at", wp.getCreatedAt());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save waypoints for " + uuid + ": " + e.getMessage());
        }
    }

    public boolean addWaypoint(Player player, String name, ChatColor color, Material icon) {
        UUID uuid = player.getUniqueId();
        Map<String, Waypoint> map = playerWaypoints.computeIfAbsent(uuid, id -> new HashMap<>());
        if (map.size() >= 50) {
            ChatUtil.sendMessage(player, "§cYou have reached the maximum limit of 50 waypoints.");
            return false;
        }

        Waypoint wp = new Waypoint(name, player.getLocation(), color, icon);
        map.put(name.toLowerCase(), wp);
        savePlayerWaypoints(uuid);

        // Sync to client mod if applicable
        sendXaeroWaypoint(player, wp);
        return true;
    }

    public boolean removeWaypoint(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Map<String, Waypoint> map = playerWaypoints.get(uuid);
        if (map == null || !map.containsKey(name.toLowerCase())) {
            return false;
        }

        map.remove(name.toLowerCase());
        if (name.equalsIgnoreCase(activeTracked.get(uuid))) {
            activeTracked.remove(uuid);
        }
        savePlayerWaypoints(uuid);
        return true;
    }

    public Waypoint getWaypoint(UUID uuid, String name) {
        Map<String, Waypoint> map = playerWaypoints.get(uuid);
        if (map == null) return null;
        return map.get(name.toLowerCase());
    }

    public Collection<Waypoint> getWaypoints(UUID uuid) {
        Map<String, Waypoint> map = playerWaypoints.get(uuid);
        if (map == null) return Collections.emptyList();
        return map.values();
    }

    public void setTracked(Player player, String name) {
        if (name == null) {
            activeTracked.remove(player.getUniqueId());
            ChatUtil.sendActionBar(player, "§7Stopped tracking waypoint.");
            return;
        }

        Waypoint wp = getWaypoint(player.getUniqueId(), name);
        if (wp == null) {
            ChatUtil.sendMessage(player, "§cWaypoint '" + name + "' not found.");
            return;
        }

        activeTracked.put(player.getUniqueId(), wp.getName().toLowerCase());
        if (player.getWorld().getName().equalsIgnoreCase(wp.getWorldName())) {
            player.setCompassTarget(new Location(player.getWorld(), wp.getX(), wp.getY(), wp.getZ()));
        }
        ChatUtil.sendMessage(player, "§aNow tracking waypoint " + wp.getColor() + "§l" + wp.getName() + "§a!");
    }

    public void toggleHud(Player player) {
        UUID u = player.getUniqueId();
        if (hudDisabled.contains(u)) {
            hudDisabled.remove(u);
            ChatUtil.sendMessage(player, "§aMinimap / Navigation HUD enabled.");
        } else {
            hudDisabled.add(u);
            ChatUtil.sendMessage(player, "§cMinimap / Navigation HUD disabled.");
        }
    }

    public boolean isHudEnabled(Player player) {
        return !hudDisabled.contains(player.getUniqueId());
    }

    private void startMinimapHudTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getSurvivalManager() == null || !plugin.getSurvivalManager().isPlayerInSurvival(player)) {
                    continue;
                }
                if (!isHudEnabled(player)) continue;

                renderMinimapHud(player);
            }
        }, 10L, 10L); // Update every 0.5s for smooth navigation
    }

    private void renderMinimapHud(Player player) {
        Location loc = player.getLocation();
        String direction = getCardinalDirection(loc.getYaw());
        String biome = formatBiome(loc.getBlock().getBiome().name());

        String tracked = activeTracked.get(player.getUniqueId());
        if (tracked != null) {
            Waypoint wp = getWaypoint(player.getUniqueId(), tracked);
            if (wp != null && loc.getWorld() != null && loc.getWorld().getName().equalsIgnoreCase(wp.getWorldName())) {
                double dist = wp.distanceTo(loc);
                String arrow = getDirectionArrow(player, wp);
                String hud = "§6" + arrow + " " + wp.getColor() + "§l" + wp.getName() + "§f: "
                        + String.format("%.0fm", dist)
                        + " §8[" + direction + "§8] §7| "
                        + "§eX:§f " + loc.getBlockX() + " §eY:§f " + loc.getBlockY() + " §eZ:§f " + loc.getBlockZ();
                ChatUtil.sendActionBar(player, hud);
                return;
            }
        }

        // Default sleek survival MiniMap HUD
        long time = loc.getWorld() != null ? loc.getWorld().getTime() : 0;
        String timeStr = (time >= 0 && time < 12000) ? "§6☀ Day" : "§9☽ Night";
        String hud = "§e🧭 §8[§b" + direction + "§8] "
                + "§7X:§f " + loc.getBlockX() + " §7Y:§f " + loc.getBlockY() + " §7Z:§f " + loc.getBlockZ()
                + " §8| §a" + biome + " §8| " + timeStr;
        ChatUtil.sendActionBar(player, hud);
    }

    private String getCardinalDirection(float yaw) {
        float rot = (yaw - 90) % 360;
        if (rot < 0) rot += 360.0;
        if (rot >= 337.5 || rot < 22.5) return "W";
        if (rot >= 22.5 && rot < 67.5) return "NW";
        if (rot >= 67.5 && rot < 112.5) return "N";
        if (rot >= 112.5 && rot < 157.5) return "NE";
        if (rot >= 157.5 && rot < 202.5) return "E";
        if (rot >= 202.5 && rot < 247.5) return "SE";
        if (rot >= 247.5 && rot < 292.5) return "S";
        return "SW";
    }

    private String getDirectionArrow(Player player, Waypoint wp) {
        Location pLoc = player.getLocation();
        double targetX = wp.getX();
        double targetZ = wp.getZ();

        double dx = targetX - pLoc.getX();
        double dz = targetZ - pLoc.getZ();
        double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = (pLoc.getYaw() % 360 + 360) % 360;
        double diff = (angleToTarget - playerYaw + 540) % 360 - 180;

        if (Math.abs(diff) < 22.5) return "⬆";
        if (diff >= 22.5 && diff < 67.5) return "⬈";
        if (diff >= 67.5 && diff < 112.5) return "➡";
        if (diff >= 112.5 && diff < 157.5) return "⬊";
        if (Math.abs(diff) >= 157.5) return "⬇";
        if (diff <= -22.5 && diff > -67.5) return "⬉";
        if (diff <= -67.5 && diff > -112.5) return "⬅";
        return "⬋";
    }

    private String formatBiome(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public void sendXaeroWaypoint(Player player, Waypoint wp) {
        // Sends waypoint data over plugin channel if client is using Xaero's Minimap mod
        try {
            byte[] data = ("xaero-waypoint:" + wp.getName() + ":" + (int)wp.getX() + ":" + (int)wp.getY() + ":" + (int)wp.getZ()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            player.sendPluginMessage(plugin, "xaerominimap:main", data);
        } catch (Throwable ignored) {}
    }
}
