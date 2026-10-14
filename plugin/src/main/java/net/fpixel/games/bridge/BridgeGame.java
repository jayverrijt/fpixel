package net.fpixel.games.bridge;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BridgeGame extends Arena {
    private final int voidY;
    private Location redSpawn;
    private Location blueSpawn;
    private Location redCage;
    private Location blueCage;
    private Location redGoal;
    private Location blueGoal;

    private final Set<UUID> redTeam = new HashSet<>();
    private final Set<UUID> blueTeam = new HashSet<>();

    private int redScore = 0;
    private int blueScore = 0;
    private final int maxScore = 5;

    private boolean inRoundCountdown = false;
    private int roundCountdown = 3;
    private final List<Location> activeCages = new ArrayList<>();
    private final Map<UUID, Integer> arrowTicks = new HashMap<>();

    public BridgeGame(FPixelPlugin plugin, String id, String displayName, int voidY) {
        super(plugin, id, displayName, "BRIDGE", 2, 2);
        this.voidY = voidY;
    }

    public void setRedSpawn(Location loc) { this.redSpawn = loc; }
    public void setBlueSpawn(Location loc) { this.blueSpawn = loc; }
    public void setRedCage(Location loc) { this.redCage = loc; }
    public void setBlueCage(Location loc) { this.blueCage = loc; }
    public void setRedGoal(Location loc) { this.redGoal = loc; }
    public void setBlueGoal(Location loc) { this.blueGoal = loc; }

    @Override
    public void onGameStart() {
        redScore = 0;
        blueScore = 0;
        redTeam.clear();
        blueTeam.clear();

        List<Player> bPlayers = getBukkitPlayers();
        if (!bPlayers.isEmpty()) redTeam.add(bPlayers.get(0).getUniqueId());
        if (bPlayers.size() > 1) {
            blueTeam.add(bPlayers.get(1).getUniqueId());
        } else if (!simulatedPlayers.isEmpty()) {
            blueTeam.add(simulatedPlayers.keySet().iterator().next());
        }

        startRound();
    }

    private void startRound() {
        inRoundCountdown = true;
        roundCountdown = 3;

        for (Player p : getBukkitPlayers()) {
            boolean isRed = redTeam.contains(p.getUniqueId());
            Location cageLoc = isRed ? redCage : blueCage;
            p.teleport(cageLoc.clone().add(0.5, 0.5, 0.5));
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.getInventory().clear();
            giveKit(p, isRed);
            spawnCage(cageLoc, isRed ? Material.RED_STAINED_GLASS : Material.BLUE_STAINED_GLASS);
        }

        for (UUID botUuid : blueTeam) {
            net.fpixel.core.testing.SimulatedPlayer bot = simulatedPlayers.get(botUuid);
            if (bot != null && blueCage != null) {
                bot.spawnEntity(blueCage.clone().add(0.5, 0.5, 0.5), "BRIDGE");
                spawnCage(blueCage, Material.BLUE_STAINED_GLASS);
            }
        }
    }

    private void giveKit(Player p, boolean isRed) {
        p.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        p.getInventory().setItem(1, new ItemStack(Material.BOW));
        p.getInventory().setItem(2, new ItemStack(Material.DIAMOND_PICKAXE));
        p.getInventory().setItem(3, new ItemStack(isRed ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA, 64));
        p.getInventory().setItem(4, new ItemStack(isRed ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA, 64));
        p.getInventory().setItem(5, new ItemStack(Material.GOLDEN_APPLE, 8));
        p.getInventory().setItem(8, new ItemStack(Material.ARROW, 1));

        p.getInventory().setChestplate(new ItemStack(isRed ? Material.LEATHER_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    private void spawnCage(Location loc, Material glassMat) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        var world = loc.getWorld();
        if (world == null) return;

        int[][] offsets = {
                {0, -1, 0}, {0, 2, 0},
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}
        };

        for (int[] off : offsets) {
            Block b = world.getBlockAt(x + off[0], y + off[1], z + off[2]);
            b.setType(glassMat, false);
            activeCages.add(b.getLocation());
        }
    }

    private void removeCages() {
        for (Location loc : activeCages) {
            loc.getBlock().setType(Material.AIR, false);
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
        }
        activeCages.clear();
    }

    @Override
    public void handleGameTick() {
        // Round countdown logic
        if (inRoundCountdown) {
            if (roundCountdown > 0) {
                for (Player p : getBukkitPlayers()) {
                    ChatUtil.sendTitle(p, "§e" + roundCountdown, "§7Cage opens soon!", 0, 20, 0);
                    ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
                roundCountdown--;
            } else {
                inRoundCountdown = false;
                removeCages();
                for (Player p : getBukkitPlayers()) {
                    ChatUtil.sendTitle(p, "§a§lCROSS!", "§7Jump into the enemy goal!", 0, 20, 5);
                    ChatUtil.playSound(p, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
                }
            }
        }

        // Void detection & Arrow recharge
        for (Player p : getBukkitPlayers()) {
            if (p.getLocation().getY() < voidY) {
                respawnPlayer(p);
            }

            // Recharge arrow every 4 seconds if player has no arrows
            if (!p.getInventory().contains(Material.ARROW)) {
                int ticks = arrowTicks.getOrDefault(p.getUniqueId(), 0) + 1;
                if (ticks >= 4) {
                    p.getInventory().setItem(8, new ItemStack(Material.ARROW, 1));
                    arrowTicks.put(p.getUniqueId(), 0);
                    ChatUtil.playSound(p, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.8f);
                } else {
                    arrowTicks.put(p.getUniqueId(), ticks);
                }
            }
        }
    }

    private void respawnPlayer(Player player) {
        boolean isRed = redTeam.contains(player.getUniqueId());
        Location spawn = isRed ? redSpawn : blueSpawn;
        player.teleport(spawn.clone().add(0.5, 1, 0.5));
        player.setHealth(20.0);
        player.setFoodLevel(20);
        ChatUtil.sendActionBar(player, "§eRespawned!");
        ChatUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME || inRoundCountdown) return;
        Player p = event.getPlayer();
        if (!contains(p)) return;

        boolean isRed = redTeam.contains(p.getUniqueId());
        Location loc = p.getLocation();

        // Check goal collision (within 3x3 pit bounds of enemy goal)
        if (isRed && blueGoal != null && isInsideGoal(loc, blueGoal)) {
            scoreGoal("RED", p);
        } else if (!isRed && redGoal != null && isInsideGoal(loc, redGoal)) {
            scoreGoal("BLUE", p);
        }
    }

    private boolean isInsideGoal(Location playerLoc, Location goalLoc) {
        if (playerLoc.getWorld() == null || !playerLoc.getWorld().equals(goalLoc.getWorld())) return false;
        double dx = Math.abs(playerLoc.getX() - goalLoc.getX());
        double dz = Math.abs(playerLoc.getZ() - goalLoc.getZ());
        double dy = playerLoc.getY() - goalLoc.getY();
        return dx <= 2.2 && dz <= 2.2 && dy >= -2.0 && dy <= 3.0;
    }

    private void scoreGoal(String team, Player scorer) {
        if (team.equals("RED")) {
            redScore++;
        } else {
            blueScore++;
        }

        // Clean up placed blocks on bridge for next round
        rollbackEngine.rollback();

        broadcast("§6§lGOAL! " + (team.equals("RED") ? "§c[RED] " : "§9[BLUE] ") + scorer.getName() + " §escored a goal! §f(" + redScore + " - " + blueScore + ")");

        for (Player p : getAllBukkitPlayers()) {
            ChatUtil.sendTitle(p, (team.equals("RED") ? "§c§lRED SCORED!" : "§9§lBLUE SCORED!"), "§f" + redScore + " §7- §f" + blueScore, 5, 40, 10);
            ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }

        if (redScore >= maxScore || blueScore >= maxScore) {
            Player winner = redScore >= maxScore ? scorer : (blueTeam.contains(scorer.getUniqueId()) ? scorer : getOtherPlayer(scorer));
            endGame(winner);
        } else {
            startRound();
        }
    }

    private Player getOtherPlayer(Player one) {
        for (Player p : getBukkitPlayers()) {
            if (!p.equals(one)) return p;
        }
        return one;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (state != GameState.IN_GAME) {
            event.setCancelled(true);
            return;
        }
        // Only allow breaking terracotta
        Material type = event.getBlock().getType();
        if (type != Material.RED_TERRACOTTA && type != Material.BLUE_TERRACOTTA) {
            event.setCancelled(true);
            return;
        }
        rollbackEngine.recordBreak(event.getBlock());
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (state != GameState.IN_GAME || inRoundCountdown) {
            event.setCancelled(true);
            return;
        }
        rollbackEngine.recordPlace(event.getBlockPlaced());
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (state != GameState.IN_GAME || inRoundCountdown) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
        event.setCancelled(true);
        respawnPlayer(event.getPlayer());
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;
        List<Player> bPlayers = getBukkitPlayers();
        if (bPlayers.size() <= 1) {
            endGame(bPlayers.isEmpty() ? null : bPlayers.get(0));
        }
    }

    @Override
    public void onGameEnd(Player winner) {
        removeCages();
    }

    @Override
    public void onArenaReset() {
        redScore = 0;
        blueScore = 0;
        redTeam.clear();
        blueTeam.clear();
        inRoundCountdown = false;
        removeCages();
        arrowTicks.clear();
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
            boolean isRed = redTeam.contains(player.getUniqueId());
            lines.add("§c[R] Goals: §f" + redScore + "/5");
            lines.add("§9[B] Goals: §f" + blueScore + "/5");
            lines.add(" ");
            lines.add("§fTeam: " + (isRed ? "§cRED" : "§9BLUE"));
        } else {
            lines.add("§fGame: §eEnding");
        }
        lines.add(" ");
        lines.add("§emc.fpixel.net");
        return lines;
    }

    public Location getBlueCage() { return blueCage; }
    public Location getBlueSpawn() { return blueSpawn; }
    public Location getRedCage() { return redCage; }
    public Location getRedSpawn() { return redSpawn; }
}
