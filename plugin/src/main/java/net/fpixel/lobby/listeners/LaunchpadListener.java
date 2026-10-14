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
        if (below.getType() == Material.REDSTONE_BLOCK || below.getType() == Material.EMERALD_BLOCK) {
            Player player = event.getPlayer();
            event.setCancelled(true);

            Location loc = player.getLocation();
            player.setVelocity(loc.getDirection().multiply(2.2).setY(1.1));

            player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
            player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        }
    }
}
