package net.fpixel.survival;

import net.fpixel.FPixelPlugin;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SurvivalListener implements Listener {
    private final FPixelPlugin plugin;

    public SurvivalListener(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo() != null ? event.getTo().getWorld() : null;

        if (fromWorld == null || toWorld == null) return;

        // Leaving Survival World for another world (Hub, Arenas, etc.)
        if (fromWorld.getName().equalsIgnoreCase("survival") && !toWorld.getName().equalsIgnoreCase("survival")) {
            if (plugin.getSurvivalManager().isPlayerInSurvival(player)) {
                plugin.getSurvivalManager().getProfileManager().saveProfile(player);
                plugin.getSurvivalManager().removeSurvivalPlayer(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorldName = event.getFrom().getName();
        String currentWorldName = player.getWorld().getName();

        // Returning to Lobby from Survival
        if (fromWorldName.equalsIgnoreCase("survival") && !currentWorldName.equalsIgnoreCase("survival")) {
            if (plugin.getGameManager().getPlayerArena(player) == null) {
                plugin.getLobbyManager().giveLobbyItems(player);
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSurvivalManager().isPlayerInSurvival(player)) {
            plugin.getSurvivalManager().getProfileManager().saveProfile(player);
            plugin.getSurvivalManager().removeSurvivalPlayer(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // If respawning in survival, ensure the player stays registered in survival mode
        if (event.getRespawnLocation().getWorld() != null &&
                event.getRespawnLocation().getWorld().getName().equalsIgnoreCase("survival")) {
            plugin.getSurvivalManager().addSurvivalPlayer(player);
        }
    }
}
