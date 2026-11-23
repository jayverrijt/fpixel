package net.fpixel.survival;

import net.fpixel.FPixelPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Provides immediate feedback when players attempt to plant crops on Farmland in dark areas
 * where vanilla Minecraft light level requirements (minimum light level 9) are not met.
 */
public class CropPlantListener implements Listener {

    private final FPixelPlugin plugin;

    private static final Set<Material> CROPS = Set.of(
            Material.POTATO,
            Material.CARROT,
            Material.WHEAT_SEEDS,
            Material.BEETROOT_SEEDS,
            Material.PUMPKIN_SEEDS,
            Material.MELON_SEEDS,
            Material.TORCHFLOWER_SEEDS,
            Material.PITCHER_POD
    );

    public CropPlantListener(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.FARMLAND) return;
        if (event.getBlockFace() != BlockFace.UP) return;

        ItemStack item = event.getItem();
        if (item == null || !CROPS.contains(item.getType())) return;

        Block above = clickedBlock.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR || above.getType() == Material.CAVE_AIR) {
            byte light = above.getLightLevel();
            if (light < 9) {
                Player player = event.getPlayer();
                player.sendActionBar(Component.text("§c💡 Te donker om te planten (lichtniveau " + light + "/15). Minimaal niveau 9 vereist (plaats een fakkel)!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            }
        }
    }
}
