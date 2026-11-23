package net.fpixel.lobby.listeners;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.game.Arena;
import net.fpixel.games.tntrun.TNTRunGame;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class DoubleJumpListener implements Listener {
    private final FPixelPlugin plugin;

    public DoubleJumpListener(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Arena arena = plugin.getGameManager().getPlayerArena(player);
        if (arena instanceof TNTRunGame tntRun) {
            tntRun.handleDoubleJump(event);
            return;
        }

        if (arena == null && player.getWorld().equals(plugin.getLobbyManager().getSpawnLocation().getWorld())) {
            Rank rank = plugin.getRankManager().getRank(player);
            boolean hasFlyRank = (rank != null && rank != Rank.MEMBER) || player.hasPermission("fpixel.fly");

            if (hasFlyRank) {
                // Ranked players can fly freely in the Lobby!
                event.setCancelled(false);
                player.setFlying(event.isFlying());
                return;
            }

            // Normal members do a double jump booster
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);

            player.setVelocity(player.getLocation().getDirection().multiply(1.4).setY(0.85));
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.3f);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.ADVENTURE) return;

        if (plugin.getGameManager().getPlayerArena(player) == null &&
                player.getWorld().equals(plugin.getLobbyManager().getSpawnLocation().getWorld())) {
            // Re-enable flight capability when touching ground
            if (player.isOnGround() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        }
    }
}
