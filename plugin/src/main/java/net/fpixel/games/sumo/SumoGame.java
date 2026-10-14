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
    private int score1 = 0;
    private int score2 = 0;
    private int currentRound = 1;
    private static final int MAX_WINS = 2; // Best of 3

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

        List<Player> playerList = getBukkitPlayers();
        if (playerList.size() >= 2) {
            player1 = playerList.get(0).getUniqueId();
            player2 = playerList.get(1).getUniqueId();
        }

        startRound();
    }

    private void startRound() {
        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = Bukkit.getPlayer(player2);

        ItemStack sumoStick = new ItemBuilder(Material.STICK)
                .name("§6§lSumo Stick")
                .addEnchant(Enchantment.KNOCKBACK, 2)
                .build();

        if (p1 != null) {
            p1.getInventory().clear();
            p1.getInventory().setItem(0, sumoStick);
            p1.setHealth(20.0);
            p1.setFoodLevel(20);
            if (spawn1 != null) p1.teleport(spawn1);
            ChatUtil.sendTitle(p1, "§eROUND " + currentRound, "§7First to knock off opponent wins!", 5, 30, 5);
        }

        if (p2 != null) {
            p2.getInventory().clear();
            p2.getInventory().setItem(0, sumoStick);
            p2.setHealth(20.0);
            p2.setFoodLevel(20);
            if (spawn2 != null) p2.teleport(spawn2);
            ChatUtil.sendTitle(p2, "§eROUND " + currentRound, "§7First to knock off opponent wins!", 5, 30, 5);
        }
    }

    @Override
    public void handleGameTick() {
        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() <= deathY) {
                handleFall(p);
            }
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player player = event.getPlayer();
        if (player.getLocation().getY() <= deathY) {
            handleFall(player);
        }
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Zero damage so players don't die of health loss, only knockback
        event.setDamage(0);
        if (event.getDamager() instanceof Player damager) {
            ChatUtil.playSound(damager, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.2f);
        }
    }

    private void handleFall(Player fallen) {
        if (state != GameState.IN_GAME) return;

        Player winner;
        if (fallen.getUniqueId().equals(player1)) {
            score2++;
            winner = Bukkit.getPlayer(player2);
        } else {
            score1++;
            winner = Bukkit.getPlayer(player1);
        }

        String winnerName = winner != null ? winner.getName() : "Opponent";
        broadcast("§a" + winnerName + " §ewon Round " + currentRound + "!");

        if (score1 >= MAX_WINS || score2 >= MAX_WINS) {
            // Match over!
            endGame(score1 >= MAX_WINS ? Bukkit.getPlayer(player1) : Bukkit.getPlayer(player2));
        } else {
            currentRound++;
            startRound();
        }
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;
        if (players.size() <= 1) {
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
        player1 = null;
        player2 = null;
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§7Sumo 1v1");
        lines.add("§fTime: §a" + String.format("%02d:%02d", gameTime / 60, gameTime % 60));
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
