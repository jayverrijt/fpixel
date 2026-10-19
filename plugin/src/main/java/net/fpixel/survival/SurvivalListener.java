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

        boolean fromSurvival = plugin.getSurvivalManager().isSurvivalWorld(fromWorld);
        boolean toSurvival = plugin.getSurvivalManager().isSurvivalWorld(toWorld);

        // Leaving Survival dimensions completely (e.g. going to Hub or Arenas)
        if (fromSurvival && !toSurvival) {
            if (plugin.getSurvivalManager().isPlayerInSurvival(player)) {
                plugin.getSurvivalManager().getProfileManager().saveProfile(player);
                plugin.getSurvivalManager().removeSurvivalPlayer(player);
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.getInventory().setItemInOffHand(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(org.bukkit.event.player.PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null) return;

        if (plugin.getSurvivalManager().isSurvivalWorld(fromWorld)) {
            // Keep player marked in survival mode across all portals
            plugin.getSurvivalManager().addSurvivalPlayer(player);

            if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                    // Survival Overworld -> Survival Nether
                    World nether = plugin.getSurvivalManager().getNetherWorld();
                    if (nether != null) {
                        org.bukkit.Location from = event.getFrom();
                        org.bukkit.Location dest = new org.bukkit.Location(nether, from.getX() / 8.0, Math.min(120, Math.max(32, from.getY())), from.getZ() / 8.0, from.getYaw(), from.getPitch());
                        event.setTo(dest);
                        event.setCanCreatePortal(true);
                    }
                } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                    // Survival Nether -> Survival Overworld (MUST RETURN TO SURVIVAL WORLD, NOT HUB!)
                    World survivalWorld = plugin.getSurvivalManager().getSurvivalWorld();
                    if (survivalWorld != null) {
                        org.bukkit.Location from = event.getFrom();
                        org.bukkit.Location dest = new org.bukkit.Location(survivalWorld, from.getX() * 8.0, Math.min(250, Math.max(60, from.getY())), from.getZ() * 8.0, from.getYaw(), from.getPitch());
                        event.setTo(dest);
                        event.setCanCreatePortal(true);
                    }
                }
            } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                    World end = plugin.getSurvivalManager().getEndWorld();
                    if (end != null) {
                        event.setTo(end.getSpawnLocation());
                    }
                } else if (fromWorld.getEnvironment() == World.Environment.THE_END) {
                    World survivalWorld = plugin.getSurvivalManager().getSurvivalWorld();
                    if (survivalWorld != null) {
                        org.bukkit.Location bedSpawn = player.getRespawnLocation();
                        event.setTo(bedSpawn != null ? bedSpawn : survivalWorld.getSpawnLocation());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        boolean fromSurvival = plugin.getSurvivalManager().isSurvivalWorld(event.getFrom());
        boolean currentSurvival = plugin.getSurvivalManager().isSurvivalWorld(player.getWorld());

        // Returning to Lobby from Survival
        if (fromSurvival && !currentSurvival) {
            if (plugin.getGameManager().getPlayerArena(player) == null) {
                plugin.getLobbyManager().giveLobbyItems(player);
                player.setGameMode(GameMode.ADVENTURE);
            }
        } else if (!fromSurvival && currentSurvival) {
            // Entering survival: remove lobby scoreboard
            plugin.getScoreboardManager().remove(player);
            plugin.getSurvivalManager().addSurvivalPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSurvivalManager().isSurvivalWorld(player.getWorld()) || plugin.getSurvivalManager().isPlayerInSurvival(player)) {
            plugin.getSurvivalManager().getProfileManager().saveProfile(player);
            plugin.getSurvivalManager().removeSurvivalPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSurvivalManager().isPlayerInSurvival(player)) {
            World respawnWorld = event.getRespawnLocation().getWorld();
            if (respawnWorld == null || !plugin.getSurvivalManager().isSurvivalWorld(respawnWorld)) {
                // If respawn is in Hub world, redirect to bed in survival or survival world spawn
                World survivalWorld = plugin.getSurvivalManager().getSurvivalWorld();
                if (survivalWorld != null) {
                    org.bukkit.Location bed = player.getRespawnLocation();
                    event.setRespawnLocation(bed != null ? bed : survivalWorld.getSpawnLocation());
                }
            }
            plugin.getSurvivalManager().addSurvivalPlayer(player);
            plugin.getScoreboardManager().remove(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg.startsWith("//")) {
            String singleSlash = "/" + msg.substring(2);
            String lower = singleSlash.toLowerCase();
            if (lower.startsWith("/cleanwaterandglass") || lower.startsWith("/cleanwaterglass")
                    || lower.startsWith("/cleanwater") || lower.startsWith("/cleanglasswater")
                    || lower.startsWith("/cleansponge") || lower.startsWith("/spongeclean")
                    || lower.startsWith("/cleanglass") || lower.startsWith("/glassclean")
                    || lower.startsWith("/glasswall") || lower.startsWith("/glazenmuur")
                    || lower.startsWith("/fillchunk") || lower.startsWith("/fillchunck")
                    || lower.startsWith("/floodfill") || lower.startsWith("/fillflood")
                    || lower.startsWith("/pos1") || lower.startsWith("/pos2")
                    || lower.startsWith("/hpos1") || lower.startsWith("/hpos2")
                    || lower.startsWith("/wand") || lower.startsWith("/set")
                    || lower.startsWith("/replace") || lower.startsWith("/undo")) {
                event.setMessage(singleSlash);
            }
        }
    }
}
