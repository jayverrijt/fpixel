package net.fpixel.core.selection;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {
    private final FPixelPlugin plugin;
    private final SelectionManager selectionManager;

    public WandListener(FPixelPlugin plugin, SelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    private boolean checkPermission(Player player) {
        if (player.hasPermission("fpixel.admin") || player.isOp()) return true;
        Rank rank = plugin.getRankManager().getRank(player);
        return rank != null && rank.getWeight() >= 90;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!selectionManager.isWand(item)) return;

        if (!checkPermission(player)) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);
            Location loc = event.getClickedBlock().getLocation();
            selectionManager.setPos1(player.getUniqueId(), loc);
            int count = selectionManager.getSelectionBlockCount(player.getUniqueId());
            ChatUtil.sendMessage(player, "§6§lWE §8» §aEerste positie (§ePos 1§a) ingesteld op §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                    + (count > 0 ? " §7(§f" + count + " §7blokken)" : ""));
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);
            Location loc = event.getClickedBlock().getLocation();
            selectionManager.setPos2(player.getUniqueId(), loc);
            int count = selectionManager.getSelectionBlockCount(player.getUniqueId());
            ChatUtil.sendMessage(player, "§6§lWE §8» §aTweede positie (§ePos 2§a) ingesteld op §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                    + (count > 0 ? " §7(§f" + count + " §7blokken)" : ""));
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }
    }
}
