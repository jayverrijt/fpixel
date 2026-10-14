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

        // Detect block below player
        Location locBelow = player.getLocation().clone().subtract(0, 0.1, 0);
        Block block = locBelow.getBlock();

        decayBlock(block);

        // Also check if standing slightly offset
        decayBlock(player.getLocation().clone().add(0.3, -0.1, 0).getBlock());
        decayBlock(player.getLocation().clone().add(-0.3, -0.1, 0).getBlock());
        decayBlock(player.getLocation().clone().add(0, -0.1, 0.3).getBlock());
        decayBlock(player.getLocation().clone().add(0, -0.1, -0.3).getBlock());
    }

    private void decayBlock(Block block) {
        if (block == null || block.getType() == Material.AIR) return;
        if (block.getType() != Material.SAND && block.getType() != Material.GRAVEL
                && block.getType() != Material.TNT && !block.getType().name().contains("CONCRETE")) {
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

            // Check if there is TNT below the sand
            Block below = block.getRelative(0, -1, 0);
            if (below.getType() == Material.TNT) {
                rollbackEngine.logCustomChange(below);
                below.setType(Material.AIR);
            }

            if (loc.getWorld() != null) {
                loc.getWorld().playSound(loc, Sound.BLOCK_SAND_BREAK, 0.6f, 1.2f);
            }
            decayingBlocks.remove(loc);
        }, 8L);
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
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7TNT Run");
        lines.add("§fTime: §a" + String.format("%02d:%02d", gameTime / 60, gameTime % 60));
        lines.add("");
        lines.add("§fPlayers Left: §a" + players.size());
        lines.add("§fDouble Jumps: §b" + doubleJumps.getOrDefault(player.getUniqueId(), 0));
        lines.add(" ");
        lines.add("§emc.fpixel.net");
        return lines;
    }

    public int getDeathY() { return deathY; }
}
