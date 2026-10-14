package net.fpixel.game;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class GameManager implements Listener {
    private final FPixelPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    public GameManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startGameTicker();
    }

    private void startGameTicker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Arena arena : arenas.values()) {
                try {
                    arena.tick();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error ticking arena " + arena.getId() + ": " + e.getMessage());
                }
            }
        }, 20L, 20L); // Tick every 1 second
    }

    public void registerArena(Arena arena) {
        arenas.put(arena.getId().toLowerCase(), arena);
        plugin.getLogger().info("Registered arena: " + arena.getId() + " (" + arena.getGameType() + ")");
    }

    public Arena getArena(String id) {
        return arenas.get(id.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Arena getPlayerArena(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(player)) {
                return arena;
            }
        }
        return null;
    }

    public boolean joinQueue(Player player, String gameType) {
        // Leave current arena if any
        Arena current = getPlayerArena(player);
        if (current != null) {
            current.leave(player);
        }

        // Find best arena (starting first, then waiting with most players)
        Arena best = null;
        for (Arena arena : arenas.values()) {
            if (!arena.getGameType().equalsIgnoreCase(gameType)) continue;
            if (arena.getState() != GameState.WAITING && arena.getState() != GameState.STARTING) continue;
            if (arena.getPlayerCount() >= arena.getMaxPlayers()) continue;

            if (best == null) {
                best = arena;
            } else if (arena.getState() == GameState.STARTING && best.getState() != GameState.STARTING) {
                best = arena;
            } else if (arena.getPlayerCount() > best.getPlayerCount()) {
                best = arena;
            }
        }

        if (best != null) {
            boolean joined = best.join(player);
            if (joined && plugin.getPartyManager() != null) {
                var party = plugin.getPartyManager().getParty(player);
                if (party != null && party.isLeader(player)) {
                    party.warpToGame(best);
                }
            }
            return joined;
        } else {
            ChatUtil.sendMessage(player, "§cNo open arena found for " + gameType + "! Please try again soon.");
            return false;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = getPlayerArena(player);
        if (arena != null) {
            arena.leave(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Arena arena = getPlayerArena(player);
        if (arena != null) {
            if (arena.getState() != GameState.IN_GAME) {
                event.setCancelled(true);
                return;
            }
            arena.onBlockBreak(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Arena arena = getPlayerArena(player);
        if (arena != null) {
            if (arena.getState() != GameState.IN_GAME) {
                event.setCancelled(true);
                return;
            }
            arena.onBlockPlace(event);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player target) {
            Arena damagerArena = getPlayerArena(damager);
            Arena targetArena = getPlayerArena(target);
            if (damagerArena == null || targetArena == null || damagerArena != targetArena) {
                event.setCancelled(true);
                return;
            }
            if (damagerArena.getState() != GameState.IN_GAME) {
                event.setCancelled(true);
                return;
            }
            damagerArena.onPlayerDamage(event);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = getPlayerArena(player);
        if (arena != null) {
            arena.onPlayerDeath(event);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = getPlayerArena(player);
        if (arena != null) {
            arena.onPlayerMove(event);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Arena arena = getPlayerArena(player);
        if (arena != null) {
            arena.onPlayerInteract(event);
        }
    }
}
