package net.fpixel.games.parkour;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class ParkourGame extends Arena {
    private final int voidY;
    private final List<Location> checkpoints = new ArrayList<>();
    private Location finishLocation;
    private Location startLocation;

    private final Map<UUID, Integer> playerProgress = new HashMap<>();

    public ParkourGame(FPixelPlugin plugin, String id, String displayName, int minPlayers, int maxPlayers, int voidY) {
        super(plugin, id, displayName, "PARKOUR", minPlayers, maxPlayers);
        this.voidY = voidY;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public void addCheckpoint(Location checkpoint) {
        this.checkpoints.add(checkpoint);
    }

    public void setFinishLocation(Location finishLocation) {
        this.finishLocation = finishLocation;
    }

    @Override
    public void onPlayerJoin(Player player) {
        player.getInventory().setItem(8, new net.fpixel.core.util.ItemBuilder(Material.RED_BED)
                .name("§c§lBack to Lobby §7(Right Click)")
                .lore("§7Right click to return to the Hub.")
                .build());
        ChatUtil.sendMessage(player, " ");
        ChatUtil.sendMessage(player, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(player, "§a§l           PARKOUR RACE");
        ChatUtil.sendMessage(player, "§7  Sprint and jump through the obstacles to reach the goal!");
        ChatUtil.sendMessage(player, "§7• Step on §aPressure Plates §7to save your Checkpoint.");
        ChatUtil.sendMessage(player, "§7• Falling will teleport you back to your last Checkpoint.");
        ChatUtil.sendMessage(player, "§e• Use your §c§lRed Bed §ein slot 9 or type §b/hub §eto leave.");
        ChatUtil.sendMessage(player, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(player, " ");
    }

    @Override
    public void onGameStart() {
        playerProgress.clear();
        for (Player p : getBukkitPlayers()) {
            playerProgress.put(p.getUniqueId(), 0);
            p.getInventory().clear();
            p.getInventory().setItem(8, new net.fpixel.core.util.ItemBuilder(Material.RED_BED)
                    .name("§c§lBack to Lobby §7(Right Click)")
                    .lore("§7Right click to return to the Hub.")
                    .build());
            p.setGameMode(GameMode.ADVENTURE);
            if (startLocation != null) {
                p.teleport(startLocation.clone().add(0.5, 1, 0.5));
            }
            ChatUtil.sendTitle(p, "§a§lRACE START!", "§eReach the finish line first!", 5, 30, 10);
            ChatUtil.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, 0.8f, 1.2f);
            ChatUtil.sendMessage(p, "§eUse your §c§lRed Bed §e(slot 9) or type §b/hub §eto return to the Lobby.");
        }
    }

    @Override
    public void handleGameTick() {
        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() < voidY) {
                respawnAtCheckpoint(p);
            }
        }
    }

    private void respawnAtCheckpoint(Player player) {
        int cpIndex = playerProgress.getOrDefault(player.getUniqueId(), 0);
        Location target = (cpIndex > 0 && cpIndex <= checkpoints.size())
                ? checkpoints.get(cpIndex - 1)
                : startLocation;

        if (target != null) {
            player.setFallDistance(0);
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.teleport(target.clone().add(0.5, 1, 0.5));
            ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
            ChatUtil.sendActionBar(player, "§cRespawned at Checkpoint " + cpIndex + "!");
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player p = event.getPlayer();
        if (!contains(p)) return;

        Location loc = p.getLocation();

        // 1. Check finish line
        if (finishLocation != null && loc.distanceSquared(finishLocation) <= 3.0) {
            broadcast("§6§lPARKOUR §8» §a" + p.getName() + " §ehas crossed the finish line in §6" + gameTime + "s§e!");
            spawnFirework(loc);
            endGame(p);
            return;
        }

        // 2. Check next checkpoint
        int current = playerProgress.getOrDefault(p.getUniqueId(), 0);
        if (current < checkpoints.size()) {
            Location nextCp = checkpoints.get(current);
            if (loc.distanceSquared(nextCp) <= 3.0) {
                int newCp = current + 1;
                playerProgress.put(p.getUniqueId(), newCp);
                ChatUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
                ChatUtil.sendTitle(p, "§a§lCHECKPOINT " + newCp, "§7" + newCp + " / " + checkpoints.size(), 0, 20, 5);
                ChatUtil.sendActionBar(p, "§aCheckpoint " + newCp + " / " + checkpoints.size() + " reached!");
                spawnFirework(nextCp);
            }
        }

        // 3. Dynamic Interactive Map Pads (Launch Pads & Speed Boosters)
        Material standOn = loc.clone().subtract(0, 0.5, 0).getBlock().getType();
        if (standOn == Material.SLIME_BLOCK || standOn == Material.EMERALD_BLOCK) {
            p.setVelocity(p.getLocation().getDirection().multiply(0.75).setY(0.6));
            ChatUtil.playSound(p, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.4f);
            ChatUtil.sendActionBar(p, "§a⚡ Slime Springboard! ⚡");
            p.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, loc, 15, 0.2, 0.2, 0.2, 0.1);
        } else if (standOn == Material.GOLD_BLOCK || standOn == Material.REDSTONE_BLOCK) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 80, 1, false, false, true));
            ChatUtil.playSound(p, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.8f);
            ChatUtil.sendActionBar(p, "§e⚡ Speed Boost! ⚡");
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setCancelled(true);
        respawnAtCheckpoint(event.getPlayer());
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;
        if (getAlivePlayerCount() <= 1) {
            endGame(getBukkitPlayers().isEmpty() ? null : getBukkitPlayers().get(0));
        }
    }

    @Override
    public void onGameEnd(Player winner) {
        // Winner celebrated
    }

    @Override
    public void onArenaReset() {
        playerProgress.clear();
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7" + id);
        lines.add(" ");
        if (state == GameState.WAITING || state == GameState.STARTING) {
            lines.add("§fPlayers: §a" + players.size() + "/" + maxPlayers);
            lines.add("§fStarting in: §a" + countdown + "s");
        } else if (state == GameState.IN_GAME) {
            int cp = playerProgress.getOrDefault(player.getUniqueId(), 0);
            lines.add("§fProgress: §a" + cp + "/" + checkpoints.size());
            lines.add("§fTime: §e" + (gameTime / 60) + ":" + String.format("%02d", gameTime % 60));
        } else {
            lines.add("§fGame: §eEnding");
        }
        lines.add(" ");
        lines.add("§emc.fpixel.nl");
        return lines;
    }
}
