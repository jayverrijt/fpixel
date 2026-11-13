package net.fpixel.lobby.portal;

import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class HubPortalListener implements Listener {
    private final HubPortalManager portalManager;

    public HubPortalListener(HubPortalManager portalManager) {
        this.portalManager = portalManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        portalManager.handlePlayerMove(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof TextDisplay td) {
            if (td.getScoreboardTags().contains("fpixel_portal_holo")) {
                portalManager.handleHologramClick(event.getPlayer(), td);
            }
        }
    }
}
