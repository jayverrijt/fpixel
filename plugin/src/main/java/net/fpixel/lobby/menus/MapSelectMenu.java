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

        // Java Chest GUI
        String title = "§8Select Map: " + cleanType;
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

        // Display up to 21 maps in rows 1, 2, 3 (slots 10-16, 19-25, 28-34)
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

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
            for (int i = 0; i < Math.min(gameArenas.size(), slots.length); i++) {
                Arena arena = gameArenas.get(i);
                boolean isJoinable = arena.getState() == GameState.WAITING || arena.getState() == GameState.STARTING || (arena.getState() == GameState.IN_GAME && arena.getPlayerCount() == 0);
                String statusColor = isJoinable ? "§a" : "§c";
                String actionText = isJoinable ? "§e▶ Click to play on this map!" : "§b▶ Click to spectate this map!";

                ItemStack mapItem = new ItemBuilder(icon)
                        .name("§e§l" + arena.getDisplayName())
                        .lore(
                                "§7Status: " + statusColor + (isJoinable && arena.getPlayerCount() == 0 ? "WAITING" : arena.getState().getDisplay()),
                                "§7Players: §a" + arena.getPlayerCount() + "§7/§a" + arena.getMaxPlayers(),
                                "§7Game Mode: §f" + (arena.getMaxPlayers() <= 2 ? "1v1 Duel" : "Multiplayer"),
                                "",
                                actionText
                        )
                        .build();
                inv.setItem(slots[i], mapItem);
            }
        }

        // Back to Main Navigator item
        inv.setItem(40, new ItemBuilder(Material.BARRIER).name("§c« Back to Navigator").build());

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
                boolean isJoinable = arena.getState() == GameState.WAITING || arena.getState() == GameState.STARTING || (arena.getState() == GameState.IN_GAME && arena.getPlayerCount() == 0);
                String status = isJoinable ? "§aWAITING" : "§c" + arena.getState().getDisplay();
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
        if (slot == 40) {
            GameSelectorMenu.open(plugin, player);
            return;
        }

        String rawTitle = ChatColor.stripColor(title);
        String gameType = rawTitle.replace("Select Map: ", "").trim().toUpperCase();

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

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot && i < gameArenas.size()) {
                Arena chosen = gameArenas.get(i);
                player.closeInventory();
                joinChosenArena(plugin, player, chosen);
                return;
            }
        }
    }

    public static void joinChosenArena(FPixelPlugin plugin, Player player, Arena chosen) {
        // If empty running arena, reset first so player can start fresh match
        if (chosen.getState() == GameState.IN_GAME && chosen.getPlayerCount() == 0) {
            chosen.resetArena();
        }

        // If player is in a party, check leadership
        if (plugin.getPartyManager() != null) {
            var party = plugin.getPartyManager().getParty(player);
            if (party != null && !party.isLeader(player)) {
                ChatUtil.sendMessage(player, "§cOnly the party leader can select minigame maps!");
                return;
            }
            if (party != null && party.isLeader(player)) {
                Arena current = plugin.getGameManager().getPlayerArena(player);
                if (current != null) {
                    current.leave(player);
                }
                party.warpToGame(chosen);
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
            ChatUtil.sendMessage(player, "§aJoined §e" + chosen.getDisplayName() + "§a!");
        }
    }
}
