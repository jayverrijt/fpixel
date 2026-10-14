package net.fpixel.lobby.listeners;

import net.fpixel.FPixelPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LaunchpadListener implements Listener {
    private final FPixelPlugin plugin;

    public LaunchpadListener(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLaunchpad(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;

        Block plate = event.getClickedBlock();
        if (plate == null) return;

        String matName = plate.getType().name();
        if (!matName.contains("PRESSURE_PLATE")) return;

        Block below = plate.getRelative(0, -1, 0);
        Material belowType = below.getType();
        if (belowType == Material.REDSTONE_BLOCK || belowType == Material.EMERALD_BLOCK ||
            belowType == Material.SLIME_BLOCK || belowType == Material.GOLD_BLOCK) {
            Player player = event.getPlayer();
            event.setCancelled(true);

            Location loc = player.getLocation();
            player.setVelocity(loc.getDirection().multiply(2.4).setY(1.15));

            player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
            player.getWorld().spawnParticle(Particle.FIREWORK, loc, 20, 0.3, 0.3, 0.3, 0.08);
            player.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.4f);
            player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        }
    }
}
