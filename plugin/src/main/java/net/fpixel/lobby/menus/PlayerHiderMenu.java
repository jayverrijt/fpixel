package net.fpixel.lobby.menus;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PlayerHiderMenu {
    public static final String TITLE = "§8Player Visibility";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatUtil.component(TITLE));

        inv.setItem(11, new ItemBuilder(Material.LIME_DYE)
                .name("§a§lShow All Players")
                .lore("§7All players in the lobby are visible.", "", "§eClick to select!")
                .build());

        inv.setItem(13, new ItemBuilder(Material.PURPLE_DYE)
                .name("§d§lShow VIPs & Staff Only")
                .lore("§7Only VIPs and Staff members are visible.", "", "§eClick to select!")
                .build());

        inv.setItem(15, new ItemBuilder(Material.GRAY_DYE)
                .name("§c§lHide All Players")
                .lore("§7All other players in the lobby are hidden.", "", "§eClick to select!")
                .build());

        player.openInventory(inv);
        ChatUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    public static void handleClick(FPixelPlugin plugin, Player player, int slot) {
        FPixelUser user = plugin.getUserManager().getUser(player);

        if (slot == 11) {
            user.setVisibility("ALL");
            updateVisibility(plugin, player, "ALL");
            ChatUtil.sendMessage(player, "§aAll players are now visible!");
            player.closeInventory();
        } else if (slot == 13) {
            user.setVisibility("VIPS");
            updateVisibility(plugin, player, "VIPS");
            ChatUtil.sendMessage(player, "§dOnly VIPs and Staff are now visible!");
            player.closeInventory();
        } else if (slot == 15) {
            user.setVisibility("NONE");
            updateVisibility(plugin, player, "NONE");
            ChatUtil.sendMessage(player, "§cAll players are now hidden!");
            player.closeInventory();
        }
    }

    public static void updateVisibility(FPixelPlugin plugin, Player player, String mode) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (mode.equals("ALL")) {
                player.showPlayer(plugin, other);
            } else if (mode.equals("VIPS")) {
                if (plugin.getRankManager().getRank(other).getWeight() >= 50) {
                    player.showPlayer(plugin, other);
                } else {
                    player.hidePlayer(plugin, other);
                }
            } else if (mode.equals("NONE")) {
                player.hidePlayer(plugin, other);
            }
        }
    }
}
