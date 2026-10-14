package net.fpixel.lobby.listeners;

import net.fpixel.FPixelPlugin;
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

        if (arena == null) {
            // In lobby double jump
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

        if (plugin.getGameManager().getPlayerArena(player) == null) {
            // Re-enable flight for double jump when touching ground
            if (player.isOnGround() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        }
    }
}
