package net.fpixel.games.tntrun;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.*;

public class TNTRunGame extends Arena {
    private final int deathY;
    private final Set<Location> decayingBlocks = new HashSet<>();
    private final Map<UUID, Integer> doubleJumps = new HashMap<>();

    private Location arenaSpawn;
    private int decayTicks = 8;

    public TNTRunGame(FPixelPlugin plugin, String id, String displayName, int minPlayers, int maxPlayers, int deathY) {
        super(plugin, id, displayName, "TNTRUN", minPlayers, maxPlayers);
        this.deathY = deathY;
    }

    public void setArenaSpawn(Location arenaSpawn) {
        this.arenaSpawn = arenaSpawn;
    }

    @Override
    public void onGameStart() {
        decayingBlocks.clear();
        doubleJumps.clear();
        decayTicks = 8;

        Location spawnTarget = (arenaSpawn != null) ? arenaSpawn.clone().add(0, 1, 0) : lobbySpawn;

        for (Player p : getBukkitPlayers()) {
            doubleJumps.put(p.getUniqueId(), 3);
            p.setAllowFlight(true);
            p.setFlying(false);
            p.getInventory().clear();
            p.getInventory().setItem(0, new net.fpixel.core.util.ItemBuilder(Material.FEATHER)
                    .name("§b§lDouble Jump §7(3 Left)")
                    .lore("§7Double-tap space to jump into the air!")
                    .build());
            if (spawnTarget != null) {
                p.teleport(spawnTarget);
            }
            ChatUtil.sendTitle(p, "§6§lTNT RUN", "§eRun and don't stop! The floor is falling!", 5, 40, 10);
            ChatUtil.sendMessage(p, "§aYou have §b3 Double Jumps§a! Double-tap space to use them.");
        }

        for (net.fpixel.core.testing.SimulatedPlayer bot : simulatedPlayers.values()) {
            if (spawnTarget != null) {
                bot.spawnEntity(spawnTarget, "TNTRUN");
            }
        }
    }

    @Override
    public void handleGameTick() {
        // Check alive players
        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() <= deathY) {
                eliminatePlayer(p);
            }
        }
        for (net.fpixel.core.testing.SimulatedPlayer bot : new ArrayList<>(simulatedPlayers.values())) {
            if (bot.getEntity() != null && bot.getEntity().isValid()) {
                if (bot.getEntity().getLocation().getY() <= deathY) {
                    eliminateSimulatedPlayer(bot);
                }
            }
        }

        // Timed Events: Speed Surges & Collapse
        if (gameTime == 45) {
            broadcast("§e§lSPEED SURGE §8» §aAll runners gained Speed II for 15s!");
            for (Player p : getBukkitPlayers()) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 300, 1));
                ChatUtil.sendTitle(p, "§e§lSPEED SURGE!", "§7Run faster! Floor falls quick!", 5, 30, 10);
                ChatUtil.playSound(p, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.6f);
            }
        } else if (gameTime == 90) {
            broadcast("§c§lMEGA SPEED SURGE §8» §aAll runners gained Speed III for 15s!");
            for (Player p : getBukkitPlayers()) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 300, 2));
                ChatUtil.sendTitle(p, "§c§lSPEED SURGE II!", "§6Maximum sprint mode engaged!", 5, 30, 10);
                ChatUtil.playSound(p, Sound.ENTITY_BAT_TAKEOFF, 1.2f, 1.8f);
            }
        } else if (gameTime == 120) {
            decayTicks = 4;
            broadcast("§4§lCOLLAPSE §8» §cFloor decay speed has doubled!");
            for (Player p : getAllBukkitPlayers()) {
                ChatUtil.sendTitle(p, "§4§lCOLLAPSE!", "§cBlocks fall twice as fast!", 5, 30, 10);
                ChatUtil.playSound(p, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
            }
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!players.contains(player.getUniqueId())) return;

        // Check if player fell below death threshold
        if (player.getLocation().getY() <= deathY) {
            eliminatePlayer(player);
            return;
        }

        // Keep players inside arena boundary if arena spawn is set
        if (arenaSpawn != null) {
            double distSq = Math.pow(player.getLocation().getX() - arenaSpawn.getX(), 2) + Math.pow(player.getLocation().getZ() - arenaSpawn.getZ(), 2);
            if (distSq > 19.0 * 19.0) {
                // Beyond arena rim: bounce back towards center
                org.bukkit.util.Vector toCenter = new org.bukkit.util.Vector(arenaSpawn.getX() - player.getLocation().getX(), 0.2, arenaSpawn.getZ() - player.getLocation().getZ()).normalize().multiply(0.7);
                player.setVelocity(toCenter);
                ChatUtil.sendActionBar(player, "§cStay inside the arena!");
            }
        }

        // Comprehensive footprint detection across horizontal corners and vertical offsets
        for (double ox : new double[]{-0.35, 0.0, 0.35}) {
            for (double oz : new double[]{-0.35, 0.0, 0.35}) {
                for (double oy : new double[]{0.0, -0.2, -0.6}) {
                    decayBlock(player.getLocation().clone().add(ox, oy, oz).getBlock());
                }
            }
        }
    }

    private void decayBlock(Block block) {
        if (block == null || block.getType() == Material.AIR || !block.getType().isSolid()) return;
        // Don't decay bedrock floor or barriers; glass will decay if walked on
        if (block.getType() == Material.BEDROCK || block.getType() == Material.BARRIER) {
            return;
        }

        Location loc = block.getLocation();
        if (decayingBlocks.contains(loc)) return;

        decayingBlocks.add(loc);

        // Schedule decay in 8 ticks (0.4s)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state != GameState.IN_GAME) return;

            // Log for instant rollback
            rollbackEngine.logCustomChange(block);
            block.setType(Material.AIR);

            // Check if there is TNT below
            Block below = block.getRelative(0, -1, 0);
            if (below.getType() == Material.TNT) {
                rollbackEngine.logCustomChange(below);
                below.setType(Material.AIR);
            }

            if (loc.getWorld() != null) {
                loc.getWorld().playSound(loc, Sound.BLOCK_SAND_BREAK, 0.6f, 1.2f);
            }
            decayingBlocks.remove(loc);
        }, (long) decayTicks);
    }

    public void handleDoubleJump(PlayerToggleFlightEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (!players.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        player.setFlying(false);

        int jumpsLeft = doubleJumps.getOrDefault(player.getUniqueId(), 0);
        if (jumpsLeft > 0) {
            doubleJumps.put(player.getUniqueId(), jumpsLeft - 1);
            player.setVelocity(player.getLocation().getDirection().multiply(1.3).setY(0.85));
            ChatUtil.playSound(player, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.3f);
            ChatUtil.sendActionBar(player, "§bDouble Jump! §7(" + (jumpsLeft - 1) + " left)");

            if (jumpsLeft - 1 <= 0) {
                player.setAllowFlight(false);
            }
        } else {
            player.setAllowFlight(false);
        }
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;
        if (getAlivePlayerCount() <= 1) {
            Player winner = null;
            if (!players.isEmpty()) {
                winner = Bukkit.getPlayer(players.iterator().next());
            }
            endGame(winner);
        }
    }

    @Override
    public void onGameEnd(Player winner) {
        for (UUID uuid : players) {
            FPixelUser user = plugin.getUserManager().getUser(uuid, "");
            if (winner != null && winner.getUniqueId().equals(uuid)) {
                user.addTntrunWin();
            } else {
                user.addTntrunLoss();
            }
        }
    }

    @Override
    public void onArenaReset() {
        decayingBlocks.clear();
        doubleJumps.clear();
        decayTicks = 8;
    }

    public String getNextEventString() {
        if (gameTime < 45) {
            int rem = 45 - gameTime;
            return "§fEvent: §eSpeed Surge §7(" + rem + "s)";
        } else if (gameTime < 90) {
            int rem = 90 - gameTime;
            return "§fEvent: §cSpeed Surge II §7(" + rem + "s)";
        } else if (gameTime < 120) {
            int rem = 120 - gameTime;
            return "§fEvent: §4Collapse §7(" + rem + "s)";
        } else {
            return "§fEvent: §4Rapid Decay";
        }
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7TNT Run");
        lines.add(getNextEventString());
        lines.add("");
        lines.add("§fPlayers Left: §a" + players.size());
        lines.add("§fDouble Jumps: §b" + doubleJumps.getOrDefault(player.getUniqueId(), 0));
        lines.add(" ");
        lines.add("§emc.fpixel.net");
        return lines;
    }

    public int getDeathY() { return deathY; }
}
