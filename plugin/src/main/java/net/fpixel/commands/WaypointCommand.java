package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.survival.waypoints.Waypoint;
import net.fpixel.survival.waypoints.WaypointManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WaypointCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public WaypointCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can manage waypoints.");
            return true;
        }

        WaypointManager wm = plugin.getWaypointManager();
        if (wm == null) {
            ChatUtil.sendMessage(player, "§cWaypoint system is currently unavailable.");
            return true;
        }

        if (label.equalsIgnoreCase("map") || label.equalsIgnoreCase("minimap") || label.equalsIgnoreCase("worldmap")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("hud")) {
                wm.toggleHud(player);
                return true;
            }
            openMapGui(player);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listWaypoints(player, wm);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add", "set", "create" -> {
                if (args.length < 2) {
                    ChatUtil.sendMessage(player, "§cUsage: §e/wp add <name> [color]");
                    return true;
                }
                String name = args[1];
                ChatColor color = ChatColor.AQUA;
                if (args.length >= 3) {
                    try {
                        color = ChatColor.valueOf(args[2].toUpperCase());
                    } catch (Exception ignored) {}
                }
                if (wm.addWaypoint(player, name, color, Material.COMPASS)) {
                    ChatUtil.sendMessage(player, "§aWaypoint " + color + "§l" + name + " §asaved at current location!");
                    ChatUtil.sendMessage(player, "§7Use §e/wp track " + name + " §7to guide your compass and minimap HUD!");
                }
            }
            case "del", "delete", "remove" -> {
                if (args.length < 2) {
                    ChatUtil.sendMessage(player, "§cUsage: §e/wp del <name>");
                    return true;
                }
                String name = args[1];
                if (wm.removeWaypoint(player, name)) {
                    ChatUtil.sendMessage(player, "§aWaypoint §e" + name + " §awas removed.");
                } else {
                    ChatUtil.sendMessage(player, "§cWaypoint '" + name + "' not found.");
                }
            }
            case "track", "nav", "navigate" -> {
                if (args.length < 2) {
                    ChatUtil.sendMessage(player, "§cUsage: §e/wp track <name>");
                    return true;
                }
                wm.setTracked(player, args[1]);
            }
            case "untrack", "stop" -> {
                wm.setTracked(player, null);
            }
            case "hud", "togglehud" -> {
                wm.toggleHud(player);
            }
            case "share" -> {
                if (args.length < 3) {
                    ChatUtil.sendMessage(player, "§cUsage: §e/wp share <name> <player>");
                    return true;
                }
                String wpName = args[1];
                Waypoint wp = wm.getWaypoint(player.getUniqueId(), wpName);
                if (wp == null) {
                    ChatUtil.sendMessage(player, "§cWaypoint '" + wpName + "' not found.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null || !target.isOnline()) {
                    ChatUtil.sendMessage(player, "§cPlayer '" + args[2] + "' not found or offline.");
                    return true;
                }
                wm.addWaypoint(target, wp.getName(), wp.getColor(), wp.getIcon());
                ChatUtil.sendMessage(player, "§aShared waypoint §e" + wp.getName() + " §awith §b" + target.getName() + "§a!");
                ChatUtil.sendMessage(target, "§6[Waypoint] §b" + player.getName() + " §7shared waypoint " + wp.getColor() + "§l" + wp.getName() + " §7with you! (§e/wp track " + wp.getName() + "§7)");
            }
            case "gui", "menu" -> {
                openWaypointsGui(player, wm);
            }
            default -> {
                ChatUtil.sendMessage(player, "§6§lSurvival Waypoint & Map Commands:");
                ChatUtil.sendMessage(player, " §e/wp add <name> [color] §7- Create waypoint at your location");
                ChatUtil.sendMessage(player, " §e/wp del <name> §7- Delete a waypoint");
                ChatUtil.sendMessage(player, " §e/wp list §7- View all waypoints");
                ChatUtil.sendMessage(player, " §e/wp track <name> §7- Track waypoint via compass & HUD");
                ChatUtil.sendMessage(player, " §e/wp untrack §7- Stop tracking");
                ChatUtil.sendMessage(player, " §e/wp share <name> <speler> §7- Share waypoint with friend");
                ChatUtil.sendMessage(player, " §e/wp hud §7- Toggle on-screen minimap HUD");
                ChatUtil.sendMessage(player, " §e/map §7- Open WorldMap & Explorer map");
            }
        }

        return true;
    }

    private void listWaypoints(Player player, WaypointManager wm) {
        Collection<Waypoint> waypoints = wm.getWaypoints(player.getUniqueId());
        if (waypoints.isEmpty()) {
            ChatUtil.sendMessage(player, "§7You have no waypoints saved yet. Use §e/wp add <name> §7to create one!");
            return;
        }

        ChatUtil.sendMessage(player, "§6§m--------------------------------------------------");
        ChatUtil.sendMessage(player, "§e§l              SURVIVAL WAYPOINTS");
        for (Waypoint wp : waypoints) {
            double dist = wp.distanceTo(player.getLocation());
            String distStr = dist >= 0 ? String.format("%.0fm", dist) : "Different Dimension";
            ChatUtil.sendMessage(player, " §f• " + wp.getColor() + "§l" + wp.getName()
                    + " §7(" + wp.getWorldName() + ": §e" + (int)wp.getX() + ", " + (int)wp.getY() + ", " + (int)wp.getZ() + "§7) §8- §a" + distStr);
        }
        ChatUtil.sendMessage(player, "§7Click or type §e/wp track <name> §7to navigate!");
        ChatUtil.sendMessage(player, "§6§m--------------------------------------------------");
    }

    private void openMapGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.component("§8Survival Map & Waypoints"));

        // Fill background
        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // 1. Web Map Link item
        ItemStack webMap = new ItemBuilder(Material.FILLED_MAP)
                .name("§6§lLive Interactive World Map")
                .lore("§7Open the real-time live map of the survival world",
                      "§7with zoomable satellite tiles & biomes!",
                      "",
                      "§eURL: §bhttps://fpixel.nl/map",
                      "§aClick to receive map link in chat!")
                .build();
        inv.setItem(11, webMap);

        // 2. Waypoints List item
        ItemStack wpItem = new ItemBuilder(Material.COMPASS)
                .name("§a§lWaypoints Manager")
                .lore("§7View, create and navigate to your waypoints.",
                      "§7Supports Xaero's Minimap & on-screen HUD!",
                      "",
                      "§eClick to open Waypoints Menu")
                .build();
        inv.setItem(13, wpItem);

        // 3. MiniMap HUD Toggle item
        boolean hudOn = plugin.getWaypointManager().isHudEnabled(player);
        ItemStack hudToggle = new ItemBuilder(hudOn ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(hudOn ? "§a§lMinimap HUD: §2ON" : "§7§lMinimap HUD: §cOFF")
                .lore("§7Shows live coordinates, cardinal direction,",
                      "§7biome and active waypoint tracker above hotbar.",
                      "",
                      "§eClick to toggle ON/OFF")
                .build();
        inv.setItem(15, hudToggle);

        player.openInventory(inv);
    }

    private void openWaypointsGui(Player player, WaypointManager wm) {
        Collection<Waypoint> waypoints = wm.getWaypoints(player.getUniqueId());
        int size = Math.min(54, Math.max(27, ((waypoints.size() / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(null, size, ChatUtil.component("§8My Waypoints"));

        int slot = 0;
        for (Waypoint wp : waypoints) {
            if (slot >= size) break;
            double dist = wp.distanceTo(player.getLocation());
            String distStr = dist >= 0 ? String.format("%.0fm", dist) : "Other Dimension";

            ItemStack item = new ItemBuilder(wp.getIcon())
                    .name(wp.getColor() + "§l" + wp.getName())
                    .lore("§7Dimension: §f" + wp.getWorldName(),
                          "§7Coordinates: §e" + (int)wp.getX() + ", " + (int)wp.getY() + ", " + (int)wp.getZ(),
                          "§7Distance: §a" + distStr,
                          "",
                          "§eLeft-Click to Track",
                          "§cType /wp del " + wp.getName() + " to Delete")
                    .build();
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        WaypointManager wm = plugin.getWaypointManager();
        if (wm == null) return Collections.emptyList();

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("add", "del", "list", "track", "untrack", "share", "hud", "menu"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("track") || args[0].equalsIgnoreCase("share"))) {
            for (Waypoint wp : wm.getWaypoints(player.getUniqueId())) {
                list.add(wp.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            for (ChatColor c : Arrays.asList(ChatColor.AQUA, ChatColor.GREEN, ChatColor.RED, ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.GOLD)) {
                list.add(c.name().toLowerCase());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("share")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) list.add(p.getName());
            }
        }

        List<String> filtered = new ArrayList<>();
        String cur = args[args.length - 1].toLowerCase();
        for (String s : list) {
            if (s.toLowerCase().startsWith(cur)) filtered.add(s);
        }
        return filtered;
    }
}
