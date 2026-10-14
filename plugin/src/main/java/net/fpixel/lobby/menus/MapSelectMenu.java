package net.fpixel.lobby.menus;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MapSelectMenu {

    public static void open(FPixelPlugin plugin, Player player, String gameType) {
        String title = "§8Select Map: " + gameType;
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.component(title));

        // Glass fillers
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        List<Arena> gameArenas = new ArrayList<>();
        for (Arena a : plugin.getGameManager().getArenas()) {
            if (a.getGameType().equalsIgnoreCase(gameType)) {
                gameArenas.add(a);
            }
        }

        // Display up to 10 maps in slots 9-18 or 10-16
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        Material icon = switch (gameType.toUpperCase()) {
            case "BEDWARS" -> Material.RED_BED;
            case "SKYWARS" -> Material.ENDER_EYE;
            case "TNTRUN" -> Material.TNT;
            case "BRIDGE" -> Material.RED_TERRACOTTA;
            case "PARKOUR" -> Material.DIAMOND_BOOTS;
            default -> Material.STICK;
        };

        for (int i = 0; i < Math.min(gameArenas.size(), slots.length); i++) {
            Arena arena = gameArenas.get(i);
            ItemStack mapItem = new ItemBuilder(icon)
                    .name("§e§l" + arena.getDisplayName())
                    .lore(
                            "§7Status: " + arena.getState().getDisplay(),
                            "§7Players: §a" + arena.getPlayerCount() + "§7/§a" + arena.getMaxPlayers(),
                            "",
                            "§e▶ Click to play on this map!"
                    )
                    .build();
            inv.setItem(slots[i], mapItem);
        }

        // Back to Main Navigator item
        inv.setItem(26, new ItemBuilder(Material.BARRIER).name("§c« Back to Navigator").build());

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    public static void handleClick(FPixelPlugin plugin, Player player, String title, int slot) {
        if (slot == 26) {
            GameSelectorMenu.open(plugin, player);
            return;
        }

        String gameType = title.replace("Select Map: ", "").trim();
        List<Arena> gameArenas = new ArrayList<>();
        for (Arena a : plugin.getGameManager().getArenas()) {
            if (a.getGameType().equalsIgnoreCase(gameType)) {
                gameArenas.add(a);
            }
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot && i < gameArenas.size()) {
                Arena chosen = gameArenas.get(i);
                player.closeInventory();
                if (plugin.getPartyManager() != null) {
                    var party = plugin.getPartyManager().getParty(player);
                    if (party != null && !party.isLeader(player)) {
                        ChatUtil.sendMessage(player, "§cOnly the party leader can join games!");
                        return;
                    }
                    if (party != null && party.isLeader(player)) {
                        party.warpToGame(chosen);
                        return;
                    }
                }
                chosen.join(player);
                return;
            }
        }
    }
}
