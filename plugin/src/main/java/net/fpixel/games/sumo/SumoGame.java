package net.fpixel.games.sumo;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SumoGame extends Arena {
    private Location spawn1;
    private Location spawn2;
    private final int deathY;

    private UUID player1;
    private UUID player2;
    private String player1Name = "Player 1";
    private String player2Name = "Player 2";
    private int score1 = 0;
    private int score2 = 0;
    private int currentRound = 1;
    private int roundTime = 0;
    private boolean roundEnding = false;
    private static final int MAX_WINS = 2; // Best of 3
    private final Map<UUID, Integer> combos = new HashMap<>();

    public SumoGame(FPixelPlugin plugin, String id, String displayName, int deathY) {
        super(plugin, id, displayName, "SUMO", 2, 2);
        this.deathY = deathY;
    }

    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }

    @Override
    public void onGameStart() {
        score1 = 0;
        score2 = 0;
        currentRound = 1;
        roundEnding = false;
        combos.clear();

        List<Player> playerList = getBukkitPlayers();
        if (playerList.size() >= 2) {
            player1 = playerList.get(0).getUniqueId();
            player2 = playerList.get(1).getUniqueId();
            player1Name = playerList.get(0).getName();
            player2Name = playerList.get(1).getName();
        } else if (!playerList.isEmpty() && !simulatedPlayers.isEmpty()) {
            player1 = playerList.get(0).getUniqueId();
            player1Name = playerList.get(0).getName();
            player2 = simulatedPlayers.keySet().iterator().next();
            player2Name = simulatedPlayers.get(player2).getName();
        } else if (players.size() >= 2) {
            List<UUID> uList = new ArrayList<>(players);
            player1 = uList.get(0);
            player2 = uList.get(1);
            Player p1 = Bukkit.getPlayer(player1);
            Player p2 = Bukkit.getPlayer(player2);
            if (p1 != null) player1Name = p1.getName();
            if (p2 != null) player2Name = p2.getName();
        }

        startRound();
    }

    private void startRound() {
        roundTime = 0;
        combos.clear();

        Player p1 = player1 != null ? Bukkit.getPlayer(player1) : null;
        Player p2 = player2 != null ? Bukkit.getPlayer(player2) : null;
        net.fpixel.core.testing.SimulatedPlayer bot = player2 != null ? simulatedPlayers.get(player2) : null;

        if (p1 != null) {
            p1.getInventory().clear();
            p1.setHealth(20.0);
            p1.setFoodLevel(20);
            p1.setFireTicks(0);
            p1.setFallDistance(0);
            p1.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            if (spawn1 != null) p1.teleport(spawn1);
            ChatUtil.sendTitle(p1, "§eROUND " + currentRound, "§7Fist fight! Knock opponent into the void!", 5, 30, 5);
        }

        if (p2 != null) {
            p2.getInventory().clear();
            p2.setHealth(20.0);
            p2.setFoodLevel(20);
            p2.setFireTicks(0);
            p2.setFallDistance(0);
            p2.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            if (spawn2 != null) p2.teleport(spawn2);
            ChatUtil.sendTitle(p2, "§eROUND " + currentRound, "§7Fist fight! Knock opponent into the void!", 5, 30, 5);
        } else if (bot != null && spawn2 != null) {
            bot.spawnEntity(spawn2, "SUMO");
        }
    }

    @Override
    public void handleGameTick() {
        if (!roundEnding) {
            roundTime++;

            // Timed Event: Sudden Death Platform Shrink
            if (roundTime == 35) {
                broadcast("§c§lWARNING §8» §eSumo ring shrinking in 5 seconds!");
                for (Player p : getAllBukkitPlayers()) {
                    ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
            } else if (roundTime == 40) {
                broadcast("§c§lSHRINKING §8» §cOuter ring is collapsing!");
                shrinkRing(true); // Warning: turn outer rim red
            } else if (roundTime == 43) {
                broadcast("§4§lSUDDEN DEATH §8» §cOuter ring has collapsed!");
                shrinkRing(false); // Collapse: turn to air
            }
        }

        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() <= deathY) {
                handleFall(p.getUniqueId());
            }
        }
        for (net.fpixel.core.testing.SimulatedPlayer bot : new ArrayList<>(simulatedPlayers.values())) {
            if (bot.getEntity() != null && bot.getEntity().isValid()) {
                if (bot.getEntity().getLocation().getY() <= deathY) {
                    bot.removeEntity();
                    handleFall(bot.getUuid());
                }
            }
        }
    }

    private void shrinkRing(boolean warningPhase) {
        if (spawn1 == null || spawn2 == null) return;
        Location center = spawn1.clone().add(spawn2).multiply(0.5);
        int cy = center.getBlockY() - 1; // The platform floor layer
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        var world = center.getWorld();
        if (world == null) return;

        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist >= 3.8 && dist <= 5.8) {
                    org.bukkit.block.Block b = world.getBlockAt(cx + x, cy, cz + z);
                    if (b.getType() != Material.AIR) {
                        rollbackEngine.logCustomChange(b);
                        if (warningPhase) {
                            b.setType(Material.RED_CONCRETE, false);
                        } else {
                            b.setType(Material.AIR, false);
                            world.playSound(b.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.4f, 1.2f);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (player.getLocation().getY() <= deathY) {
            handleFall(player.getUniqueId());
        }
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Zero damage so players don't die of health loss, only knockback
        event.setDamage(0);
        if (event.getDamager() instanceof Player damager) {
            ChatUtil.playSound(damager, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.2f);
            if (event.getEntity() instanceof Player victim) {
                combos.put(victim.getUniqueId(), 0);
            }
            int combo = combos.getOrDefault(damager.getUniqueId(), 0) + 1;
            combos.put(damager.getUniqueId(), combo);
            if (combo >= 3) {
                float pitch = Math.min(2.0f, 1.0f + (combo * 0.1f));
                ChatUtil.playSound(damager, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                ChatUtil.sendActionBar(damager, "§6§l" + combo + " HIT COMBO! §e⚡");
            }
        }
    }

    private synchronized void handleFall(UUID fallenUuid) {
        if (state != GameState.IN_GAME || roundEnding) return;
        roundEnding = true;

        Player fallen = Bukkit.getPlayer(fallenUuid);
        if (fallen != null) {
            fallen.setFallDistance(0);
            fallen.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            fallen.setHealth(20.0);
            fallen.setFoodLevel(20);
            Location safeLoc = fallenUuid.equals(player1) ? spawn1 : spawn2;
            if (safeLoc != null) {
                fallen.teleport(safeLoc);
            }
        }

        UUID winnerUuid;
        if (fallenUuid.equals(player1)) {
            score2++;
            winnerUuid = player2;
        } else {
            score1++;
            winnerUuid = player1;
        }

        Player winner = Bukkit.getPlayer(winnerUuid);
        String winnerName = winner != null ? winner.getName() : (winnerUuid != null && winnerUuid.equals(player1) ? player1Name : player2Name);
        broadcast("§a" + winnerName + " §ewon Round " + currentRound + "!");

        for (Player p : getAllBukkitPlayers()) {
            p.setFallDistance(0);
            p.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        }

        if (score1 >= MAX_WINS || score2 >= MAX_WINS) {
            UUID matchWinnerUuid = score1 >= MAX_WINS ? player1 : player2;
            Player matchWinner = matchWinnerUuid != null ? Bukkit.getPlayer(matchWinnerUuid) : null;
            if (matchWinner == null) {
                for (Player p : getBukkitPlayers()) {
                    if (p.getUniqueId().equals(matchWinnerUuid)) {
                        matchWinner = p;
                        break;
                    }
                }
            }
            if (matchWinner == null && !getBukkitPlayers().isEmpty()) {
                matchWinner = getBukkitPlayers().get(0);
            }
            endGame(matchWinner);
        } else {
            currentRound++;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == GameState.IN_GAME) {
                    roundEnding = false;
                    startRound();
                }
            }, 20L);
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
                user.addSumoWin();
            } else {
                user.addSumoLoss();
            }
        }
    }

    @Override
    public void onArenaReset() {
        score1 = 0;
        score2 = 0;
        currentRound = 1;
        roundTime = 0;
        roundEnding = false;
        combos.clear();
        player1 = null;
        player2 = null;
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7Sumo 1v1");
        if (roundTime < 40) {
            lines.add("§fShrink in: §e" + (40 - roundTime) + "s");
        } else {
            lines.add("§4§lSUDDEN DEATH");
        }
        lines.add("");
        lines.add("§fRound: §e" + currentRound + "/3");

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);
        String name1 = p1 != null ? p1.getName() : "P1";
        String name2 = p2 != null ? p2.getName() : "P2";

        lines.add("§f" + name1 + ": §a" + score1);
        lines.add("§f" + name2 + ": §a" + score2);
        lines.add(" ");

        FPixelUser user = plugin.getUserManager().getUser(player);
        lines.add("§fStreak: §b" + user.getSumoStreak());
        lines.add("  ");
        lines.add("§emc.fpixel.net");
        return lines;
    }

    public int getDeathY() { return deathY; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
}
