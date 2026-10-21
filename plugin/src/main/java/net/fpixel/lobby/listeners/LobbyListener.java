package net.fpixel.lobby.listeners;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.games.bedwars.BedWarsShop;
import net.fpixel.lobby.menus.GameSelectorMenu;
import net.fpixel.lobby.menus.MapSelectMenu;
import net.fpixel.lobby.menus.PlayerHiderMenu;
import net.fpixel.lobby.menus.StatsMenu;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;

public class LobbyListener implements Listener {
    private final FPixelPlugin plugin;

    public LobbyListener(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isInLobby(Player player) {
        if (plugin.getSurvivalManager() != null && plugin.getSurvivalManager().isSurvivalWorld(player.getWorld())) {
            return false;
        }
        if (player.getWorld().getName().equalsIgnoreCase("survival")) {
            return false;
        }
        return plugin.getGameManager().getPlayerArena(player) == null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null); // Silent join message, Hypixel style

        plugin.getRankManager().updatePlayerDisplayName(player);
        plugin.getLobbyManager().teleportToLobby(player);

        // Welcome Title
        Rank rank = plugin.getRankManager().getRank(player);
        ChatUtil.sendTitle(player, "§6§lFPIXEL NETWORK", "§eWelcome back, " + rank.getColor() + player.getName() + "§e!", 10, 50, 15);
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        // Chat welcome
        ChatUtil.sendMessage(player, "§aWelcome to §6§lFPixel Network§a!");
        ChatUtil.sendMessage(player, "§7Select a minigame using your §aNavigator Compass§7.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Player player = event.getPlayer();
        plugin.getScoreboardManager().remove(player);
        plugin.getUserManager().unloadUser(player.getUniqueId());
    }


    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isInLobby(player)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isInLobby(player)) {
            event.setCancelled(true);
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                plugin.getLobbyManager().teleportToLobby(player);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(player)) {
            Location spawn = plugin.getLobbyManager().getSpawnLocation();
            Location loc = player.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(spawn.getWorld())) {
                double dx = loc.getX() - spawn.getX();
                double dz = loc.getZ() - spawn.getZ();
                double distSq = dx * dx + dz * dz;

                // Expanded grand hub radius is 180 blocks, min Y is 50, max Y is 240
                if (distSq > 180 * 180 || loc.getY() < 50 || loc.getY() > 240) {
                    plugin.getLobbyManager().teleportToLobby(player);
                    ChatUtil.sendActionBar(player, "§c§lStay inside the Hub area!");
                    ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
                }
            }
        }
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(player) && player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(player) && player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isInLobby(event.getPlayer()) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInLobby(player)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null) return;

            if (item.getType() == Material.COMPASS) {
                event.setCancelled(true);
                GameSelectorMenu.open(plugin, player);
            } else if (item.getType() == Material.PLAYER_HEAD) {
                event.setCancelled(true);
                StatsMenu.open(plugin, player);
            } else if (item.getType() == Material.LIME_DYE || item.getType() == Material.GRAY_DYE || item.getType() == Material.PURPLE_DYE) {
                event.setCancelled(true);
                PlayerHiderMenu.open(player);
            } else if (item.getType() == Material.NETHER_STAR) {
                event.setCancelled(true);
                net.fpixel.lobby.menus.QuickMenu.open(plugin, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Firework) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBedReturnInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.RED_BED) return;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.contains("Lobby") || name.contains("Back") || name.contains("Hub") || name.contains("Return")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena != null) {
                    arena.leave(player);
                }
                plugin.getLobbyManager().teleportToLobby(player);
                ChatUtil.sendMessage(player, "§eReturned to the Hub!");
                ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayAgainInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PAPER) return;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.contains("Play Again")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena != null) {
                    String gameType = arena.getGameType();
                    arena.leave(player);
                    ChatUtil.sendMessage(player, "§aSearching for a new §e" + gameType + " §agame...");
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getGameManager().joinQueue(player, gameType);
                        }
                    }, 5L);
                } else {
                    ChatUtil.sendMessage(player, "§cYou are not currently in a minigame!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpectatorTeleporterInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) return;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.contains("Select Map")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena != null) {
                    MapSelectMenu.open(plugin, player, arena.getGameType());
                } else {
                    GameSelectorMenu.open(plugin, player);
                }
                return;
            }
            if (name.contains("Teleporter")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena != null) {
                    List<Player> alive = arena.getAlivePlayers();
                    if (!alive.isEmpty()) {
                        Player target = alive.get(new java.util.Random().nextInt(alive.size()));
                        player.teleport(target.getLocation());
                        ChatUtil.sendMessage(player, "§aTeleported to §e" + target.getName() + "§a!");
                        ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                    } else {
                        ChatUtil.sendMessage(player, "§cNo alive players found to spectate.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSkyWarsKitInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BOW) return;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.contains("Kit") || name.contains("Select Kit")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena instanceof net.fpixel.games.skywars.SkyWarsGame sw) {
                    net.fpixel.games.skywars.SkyWarsKitMenu.open(plugin, player, sw);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() == Material.RED_BED && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Lobby") || name.contains("Back") || name.contains("Hub") || name.contains("Return")) {
                event.setCancelled(true);
                player.closeInventory();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena != null) {
                    arena.leave(player);
                }
                plugin.getLobbyManager().teleportToLobby(player);
                ChatUtil.sendMessage(player, "§eReturned to the Hub!");
                ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                return;
            }
        }

        if (clicked != null && clicked.getType() == Material.PAPER && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Play Again")) {
                event.setCancelled(true);
                player.closeInventory();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena != null) {
                    String gameType = arena.getGameType();
                    arena.leave(player);
                    ChatUtil.sendMessage(player, "§aSearching for a new §e" + gameType + " §agame...");
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getGameManager().joinQueue(player, gameType);
                        }
                    }, 5L);
                }
                return;
            }
        }

        if (clicked != null && clicked.getType() == Material.COMPASS && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Select Map")) {
                event.setCancelled(true);
                player.closeInventory();
                Arena arena = plugin.getGameManager().getPlayerArena(player);
                if (arena != null) {
                    MapSelectMenu.open(plugin, player, arena.getGameType());
                } else {
                    GameSelectorMenu.open(plugin, player);
                }
                return;
            }
        }

        if (title.contains("Game Navigator")) {
            event.setCancelled(true);
            GameSelectorMenu.handleClick(plugin, player, event.getRawSlot(), event.getClick());
        } else if (title.contains("FPixel Quick Menu")) {
            event.setCancelled(true);
            net.fpixel.lobby.menus.QuickMenu.handleClick(plugin, player, event.getRawSlot());
        } else if (title.contains("Select Map:")) {
            event.setCancelled(true);
            MapSelectMenu.handleClick(plugin, player, title, event.getRawSlot());
        } else if (title.contains("Player Visibility")) {
            event.setCancelled(true);
            PlayerHiderMenu.handleClick(plugin, player, event.getRawSlot());
        } else if (title.contains("My Profile & Stats")) {
            event.setCancelled(true);
        } else if (title.contains("BedWars Item Shop") || title.contains("BedWars Team Upgrades")) {
            event.setCancelled(true);
            BedWarsShop.handleClick(player, event.getRawSlot());
        } else if (title.contains("Select SkyWars Kit")) {
            event.setCancelled(true);
            net.fpixel.games.skywars.SkyWarsKitMenu.handleClick(plugin, player, event.getRawSlot());
        } else if (isInLobby(player) && player.getGameMode() != GameMode.CREATIVE) {
            // Lock lobby inventory
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeavesDecay(org.bukkit.event.block.LeavesDecayEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase("world")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(org.bukkit.event.entity.ItemSpawnEvent event) {
        if (event.getEntity().getWorld().getName().equalsIgnoreCase("world")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isInLobby(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
