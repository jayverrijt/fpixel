package net.fpixel.lobby.menus;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.ArrayList;
import java.util.List;

public class MapSelectMenu {

    public static void open(FPixelPlugin plugin, Player player, String gameType) {
        open(plugin, player, gameType, 1);
    }

    public static void open(FPixelPlugin plugin, Player player, String gameType, int page) {
        String cleanType = gameType.toUpperCase().trim();

        List<Arena> gameArenas = new ArrayList<>();
        for (Arena a : plugin.getGameManager().getArenas()) {
            if (a.getGameType().equalsIgnoreCase(cleanType)) {
                gameArenas.add(a);
            }
        }

        // Bedrock Form
        if (plugin.getBedrockManager() != null && plugin.getBedrockManager().isBedrock(player)) {
            openBedrockForm(plugin, player, cleanType, gameArenas);
            return;
        }

        // Slots for maps (21 slots per page in rows 1, 2, 3)
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int totalPages = Math.max(1, (int) Math.ceil((double) gameArenas.size() / slots.length));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        // Java Chest GUI with pagination indicator
        String title = "§8Select Map: " + cleanType + (totalPages > 1 ? " §7(" + page + "/" + totalPages + ")" : "");
        Inventory inv = Bukkit.createInventory(null, 45, ChatUtil.component(title));

        // Glass fillers
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 45; i++) inv.setItem(i, filler);

        // Quick join button at slot 4
        ItemStack quickJoin = new ItemBuilder(Material.NETHER_STAR)
                .name("§b§lQuick Join §7(Any Open Map)")
                .lore(
                        "§7Click to automatically join",
                        "§7the best open " + cleanType + " game.",
                        "",
                        "§e▶ Click to Quick Join!"
                )
                .build();
        inv.setItem(4, quickJoin);

        Material icon = switch (cleanType) {
            case "BEDWARS" -> Material.RED_BED;
            case "SKYWARS" -> Material.ENDER_EYE;
            case "TNTRUN" -> Material.TNT;
            case "BRIDGE" -> Material.RED_TERRACOTTA;
            case "PARKOUR" -> Material.DIAMOND_BOOTS;
            case "SUMO" -> Material.LEAD;
            case "MURDER_MYSTERY" -> Material.IRON_SWORD;
            case "PROP_HUNT" -> Material.CRAFTING_TABLE;
            default -> Material.IRON_SWORD;
        };

        if (gameArenas.isEmpty()) {
            ItemStack noMaps = new ItemBuilder(Material.BARRIER)
                    .name("§c§lNo Open Arenas")
                    .lore(
                            "§7There are currently no loaded arenas for " + cleanType + ".",
                            "",
                            "§e▶ Click to auto-generate & launch a game!"
                    )
                    .build();
            inv.setItem(22, noMaps);
        } else {
            int startIndex = (page - 1) * slots.length;
            int endIndex = Math.min(startIndex + slots.length, gameArenas.size());

            for (int i = startIndex; i < endIndex; i++) {
                Arena arena = gameArenas.get(i);
                boolean isFull = arena.getPlayerCount() >= arena.getMaxPlayers();
                boolean isJoinable = !isFull && (arena.getState() == GameState.WAITING || arena.getState() == GameState.STARTING || (arena.getState() == GameState.IN_GAME && arena.getPlayerCount() == 0));

                String statusColor;
                String statusDisplay;
                String actionText;

                if (isFull) {
                    statusColor = "§c";
                    statusDisplay = "§c§lVOL (§e" + arena.getPlayerCount() + "§7/§e" + arena.getMaxPlayers() + "§c)";
                    actionText = "§b▶ Klik om mee te kijken (Spectator)";
                } else if (isJoinable) {
                    statusColor = "§a";
                    statusDisplay = arena.getPlayerCount() == 0 ? "WAITING" : arena.getState().getDisplay();
                    actionText = "§e▶ Click to play on this map!";
                } else {
                    statusColor = "§6";
                    statusDisplay = arena.getState().getDisplay();
                    actionText = "§b▶ Click to spectate this map!";
                }

                ItemStack mapItem = new ItemBuilder(icon)
                        .name("§e§l" + arena.getDisplayName())
                        .lore(
                                "§7Status: " + statusColor + statusDisplay,
                                "§7Players: §a" + arena.getPlayerCount() + "§7/§a" + arena.getMaxPlayers(),
                                "§7Game Mode: §f" + (arena.getMaxPlayers() <= 2 ? "1v1 Duel" : "Multiplayer"),
                                "",
                                actionText
                        )
                        .build();
                inv.setItem(slots[i - startIndex], mapItem);
            }
        }

        // Navigation row (Row 5: slots 36-44)
        if (page > 1) {
            inv.setItem(38, new ItemBuilder(Material.ARROW)
                    .name("§e◀ Vorige Pagina §7(" + (page - 1) + "/" + totalPages + ")")
                    .lore("§7Klik om terug te gaan naar pagina " + (page - 1))
                    .build());
        }

        // Back to Main Navigator item at slot 40
        inv.setItem(40, new ItemBuilder(Material.BARRIER).name("§c« Back to Navigator").build());

        if (page < totalPages) {
            inv.setItem(42, new ItemBuilder(Material.ARROW)
                    .name("§eVolgende Pagina ▶ §7(" + (page + 1) + "/" + totalPages + ")")
                    .lore("§7Klik om naar pagina " + (page + 1) + " te gaan")
                    .build());
        }

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private static void openBedrockForm(FPixelPlugin plugin, Player player, String gameType, List<Arena> gameArenas) {
        try {
            FloodgatePlayer fp = plugin.getBedrockManager().getFloodgatePlayer(player);
            if (fp == null) return;

            var builder = SimpleForm.builder()
                    .title("§6§lSelect Map: " + gameType)
                    .content("Choose a map to play or spectate:")
                    .button("§b§lQuick Join §7(Any Open Map)");

            for (Arena arena : gameArenas) {
                boolean isFull = arena.getPlayerCount() >= arena.getMaxPlayers();
                boolean isJoinable = !isFull && (arena.getState() == GameState.WAITING || arena.getState() == GameState.STARTING || (arena.getState() == GameState.IN_GAME && arena.getPlayerCount() == 0));
                String status = isFull ? "§c[VOL - Spectate]" : (isJoinable ? "§aWAITING" : "§c" + arena.getState().getDisplay());
                builder.button("§e§l" + arena.getDisplayName() + "\n§7" + status + " (" + arena.getPlayerCount() + "/" + arena.getMaxPlayers() + ")");
            }

            builder.button("§c« Back to Navigator");

            builder.validResultHandler(response -> {
                int id = response.clickedButtonId();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (id == 0) {
                        Arena current = plugin.getGameManager().getPlayerArena(player);
                        if (current != null) current.leave(player);
                        plugin.getGameManager().joinQueue(player, gameType);
                    } else if (id <= gameArenas.size()) {
                        Arena chosen = gameArenas.get(id - 1);
                        joinChosenArena(plugin, player, chosen);
                    } else {
                        GameSelectorMenu.open(plugin, player);
                    }
                });
            });

            fp.sendForm(builder.build());
        } catch (Throwable t) {
            // Fallback to chest GUI
        }
    }

    public static void handleClick(FPixelPlugin plugin, Player player, String title, int slot) {
        String rawTitle = ChatColor.stripColor(title);
        // Extract gameType and page from title, e.g. "Select Map: SKYWARS (1/2)" or "Select Map: SKYWARS"
        String gameType = rawTitle.replace("Select Map: ", "").trim();
        int page = 1;
        if (gameType.contains("(") && gameType.contains(")")) {
            try {
                int pStart = gameType.indexOf('(');
                int pEnd = gameType.indexOf(')');
                String pagePart = gameType.substring(pStart + 1, pEnd).trim();
                if (pagePart.contains("/")) {
                    page = Integer.parseInt(pagePart.split("/")[0].trim());
                }
                gameType = gameType.substring(0, pStart).trim();
            } catch (Exception ignored) {}
        }
        gameType = gameType.toUpperCase().trim();

        if (slot == 40) {
            GameSelectorMenu.open(plugin, player);
            return;
        }

        if (slot == 38 && page > 1) {
            open(plugin, player, gameType, page - 1);
            return;
        }

        List<Arena> gameArenas = new ArrayList<>();
        for (Arena a : plugin.getGameManager().getArenas()) {
            if (a.getGameType().equalsIgnoreCase(gameType)) {
                gameArenas.add(a);
            }
        }

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int totalPages = Math.max(1, (int) Math.ceil((double) gameArenas.size() / slots.length));

        if (slot == 42 && page < totalPages) {
            open(plugin, player, gameType, page + 1);
            return;
        }

        // Slot 4 or Slot 22 (Empty / Quick Join)
        if (slot == 4 || slot == 22) {
            player.closeInventory();
            Arena current = plugin.getGameManager().getPlayerArena(player);
            if (current != null) {
                current.leave(player);
            }
            plugin.getGameManager().joinQueue(player, gameType);
            return;
        }

        int startIndex = (page - 1) * slots.length;

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                int mapIndex = startIndex + i;
                if (mapIndex < gameArenas.size()) {
                    Arena chosen = gameArenas.get(mapIndex);
                    player.closeInventory();
                    joinChosenArena(plugin, player, chosen);
                    return;
                }
            }
        }
    }

    public static void joinChosenArena(FPixelPlugin plugin, Player player, Arena chosen) {
        // If empty running arena, reset first so player can start fresh match
        if (chosen.getState() == GameState.IN_GAME && chosen.getPlayerCount() == 0) {
            chosen.resetArena();
        }

        // Check if player is in a party
        if (plugin.getPartyManager() != null) {
            var party = plugin.getPartyManager().getParty(player);
            if (party != null && !party.isLeader(player)) {
                ChatUtil.sendMessage(player, "§cOnly the party leader can select minigame maps!");
                return;
            }
            if (party != null && party.isLeader(player)) {
                List<Player> members = party.getOnlineMembers();
                int partySize = members.size();

                if (partySize > chosen.getMaxPlayers()) {
                    ChatUtil.sendMessage(player, "§cYour party has " + partySize + " players, but this map only holds up to " + chosen.getMaxPlayers() + " players!");
                    return;
                }

                int availableSlots = chosen.getMaxPlayers() - chosen.getPlayerCount();
                if (partySize > availableSlots) {
                    ChatUtil.sendMessage(player, "§cNot enough space in this map for your party! (" + availableSlots + " slots available, " + partySize + " in party)");
                    return;
                }

                // Warp the entire party into chosen arena (including leader)
                party.warpToGame(chosen);
                ChatUtil.sendMessage(player, "§aJoined §e" + chosen.getDisplayName() + " §awith your party (" + partySize + " players)!");
                return;
            }
        }

        // Leave current arena first if in one (e.g. switching maps in waiting lobby)
        Arena current = plugin.getGameManager().getPlayerArena(player);
        if (current != null) {
            if (current.getId().equals(chosen.getId())) {
                ChatUtil.sendMessage(player, "§cYou are already in this map!");
                return;
            }
            current.leave(player);
        }

        boolean joined = chosen.join(player);
        if (joined) {
            if (chosen.getPlayerCount() > chosen.getMaxPlayers()) {
                ChatUtil.sendMessage(player, "§eToegevoegd als §bSpectator§e op §f" + chosen.getDisplayName() + "§e.");
            } else {
                ChatUtil.sendMessage(player, "§aJoined §e" + chosen.getDisplayName() + "§a!");
            }
        }
    }
}
