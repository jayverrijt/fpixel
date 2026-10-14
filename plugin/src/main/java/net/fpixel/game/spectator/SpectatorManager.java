package net.fpixel.game.spectator;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpectatorManager {
    private final FPixelPlugin plugin;

    public SpectatorManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public void makeSpectator(Player player, Arena arena) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.getInventory().clear();

        // Give spectator items
        ItemStack compass = new ItemBuilder(Material.COMPASS)
                .name("§a§lTeleporter §7(Right Click)")
                .lore("§7Click to teleport to an alive player.")
                .build();

        ItemStack lobbyBed = new ItemBuilder(Material.RED_BED)
                .name("§c§lBack to Lobby §7(Right Click)")
                .lore("§7Click to return to the Hub.")
                .build();

        player.getInventory().setItem(0, compass);
        player.getInventory().setItem(8, lobbyBed);

        // Hide from alive players
        for (Player alive : arena.getAlivePlayers()) {
            alive.hidePlayer(plugin, player);
        }

        ChatUtil.sendMessage(player, "§7You are now spectating. Use your §aCompass §7to watch players.");
        ChatUtil.sendTitle(player, "§c§lYOU ARE SPECTATING", "§7Use the compass to track alive players", 10, 40, 10);
    }

    public void removeSpectator(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();

        // Unhide for all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(plugin, player);
        }
    }
}
