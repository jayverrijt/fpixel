package net.fpixel.lobby;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.lobby.menus.PlayerHiderMenu;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LobbyManager {
    private final FPixelPlugin plugin;
    private Location spawnLocation;

    public LobbyManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        loadSpawn();
    }

    public void loadSpawn() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("lobby.world")) {
            String world = config.getString("lobby.world");
            double x = config.getDouble("lobby.x");
            double y = config.getDouble("lobby.y");
            double z = config.getDouble("lobby.z");
            float yaw = (float) config.getDouble("lobby.yaw");
            float pitch = (float) config.getDouble("lobby.pitch");
            this.spawnLocation = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
        } else {
            this.spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        if (this.spawnLocation != null && this.spawnLocation.getWorld() != null) {
            for (org.bukkit.entity.BlockDisplay bd : this.spawnLocation.getWorld().getEntitiesByClass(org.bukkit.entity.BlockDisplay.class)) {
                if (bd.getLocation().distance(this.spawnLocation) < 250) {
                    bd.remove();
                }
            }
        }
    }

    public void setSpawn(Location location) {
        this.spawnLocation = location;
        FileConfiguration config = plugin.getConfig();
        config.set("lobby.world", location.getWorld().getName());
        config.set("lobby.x", location.getX());
        config.set("lobby.y", location.getY());
        config.set("lobby.z", location.getZ());
        config.set("lobby.yaw", location.getYaw());
        config.set("lobby.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public Location getSpawnLocation() {
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return spawnLocation;
    }

    public void giveLobbyItems(Player player) {
        player.getInventory().clear();

        // Slot 0: Game Navigator
        ItemStack compass = new ItemBuilder(Material.COMPASS)
                .name("§a§lGame Navigator §7(Right Click)")
                .lore("§7Click to select and play minigames.")
                .build();
        player.getInventory().setItem(0, compass);

        // Slot 4: Profile / Stats Head
        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(player.getName())
                .name("§e§lMy Profile §7(Right Click)")
                .lore("§7Click to view your stats and levels.")
                .build();
        player.getInventory().setItem(4, head);

        // Slot 7: Player Hider
        ItemStack hider = new ItemBuilder(Material.LIME_DYE)
                .name("§b§lPlayers: §aVisible §7(Right Click)")
                .lore("§7Click to change player visibility in the lobby.")
                .build();
        player.getInventory().setItem(7, hider);

        // Slot 8: Quick Menu / Bedrock form
        ItemStack menu = new ItemBuilder(Material.NETHER_STAR)
                .name("§6§lQuick Menu §7(Right Click)")
                .lore("§7Click to open quick actions and lobby selection.")
                .build();
        player.getInventory().setItem(8, menu);
    }

    public void teleportToLobby(Player player) {
        if (plugin.getSurvivalManager() != null) {
            if (plugin.getSurvivalManager().isSurvivalWorld(player.getWorld()) || plugin.getSurvivalManager().isPlayerInSurvival(player)) {
                plugin.getSurvivalManager().getProfileManager().saveProfile(player);
                plugin.getSurvivalManager().removeSurvivalPlayer(player);
            }
        }

        player.teleport(getSpawnLocation());
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setAllowFlight(true);
        player.setFlying(false);

        // Clear all active potion effects (especially Invisibility from minigames)
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Clean up any rogue block display entities that followed or are near the player
        for (org.bukkit.entity.Entity nearby : player.getNearbyEntities(8, 8, 8)) {
            if (nearby instanceof org.bukkit.entity.BlockDisplay bd) {
                bd.remove();
            }
        }

        giveLobbyItems(player);

        FPixelUser user = plugin.getUserManager().getUser(player);
        PlayerHiderMenu.updateVisibility(plugin, player, user.getVisibility());

        ChatUtil.sendActionBar(player, "§aTeleported to Hub!");
        ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
    }

}
