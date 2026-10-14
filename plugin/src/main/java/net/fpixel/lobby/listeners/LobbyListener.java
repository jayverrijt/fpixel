package net.fpixel.lobby.listeners;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.games.bedwars.BedWarsShop;
import net.fpixel.lobby.menus.GameSelectorMenu;
import net.fpixel.lobby.menus.PlayerHiderMenu;
import net.fpixel.lobby.menus.StatsMenu;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        return plugin.getGameManager().getPlayerArena(player) == null;
    }

    @EventHandler
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
        if (isInLobby(player) && player.getLocation().getY() < -10) {
            plugin.getLobbyManager().teleportToLobby(player);
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
                GameSelectorMenu.open(plugin, player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.contains("Game Navigator")) {
            event.setCancelled(true);
            GameSelectorMenu.handleClick(plugin, player, event.getRawSlot());
        } else if (title.contains("Player Visibility")) {
            event.setCancelled(true);
            PlayerHiderMenu.handleClick(plugin, player, event.getRawSlot());
        } else if (title.contains("My Profile & Stats")) {
            event.setCancelled(true);
        } else if (title.contains("BedWars Item Shop")) {
            event.setCancelled(true);
            BedWarsShop.handleClick(player, event.getRawSlot());
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
}
