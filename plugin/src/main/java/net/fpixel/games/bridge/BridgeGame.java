package net.fpixel.games.bridge;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.Bukkit;
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
    private final Map<UUID, Integer> kills = new HashMap<>();

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
        rollbackEngine.rollback();

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
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        var world = loc.getWorld();
        if (world == null) return;

        // Ensure permanent solid platform floor directly under cage
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(cx + dx, cy - 1, cz + dz).setType(Material.SMOOTH_STONE, false);
            }
        }

        // Build temporary glass enclosure (walls dy = 0..2, ceiling dy = 3)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                        Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                        b.setType(glassMat, false);
                        activeCages.add(b.getLocation());
                    } else {
                        Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                        b.setType(Material.AIR, false);
                    }
                }
                Block top = world.getBlockAt(cx + dx, cy + 3, cz + dz);
                top.setType(glassMat, false);
                activeCages.add(top.getLocation());
            }
        }
    }

    private void removeCages() {
        for (Location loc : activeCages) {
            loc.getBlock().setType(Material.AIR, false);
            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
        }
        activeCages.clear();
    }

    private void removeCageAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        var world = loc.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    Location bLoc = new Location(world, cx + dx, cy + dy, cz + dz);
                    if (activeCages.remove(bLoc)) {
                        bLoc.getBlock().setType(Material.AIR, false);
                    }
                }
            }
        }
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
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

        // Void detection for simulated bots in testing mode
        for (net.fpixel.core.testing.SimulatedPlayer bot : simulatedPlayers.values()) {
            if (bot.getEntity() != null && bot.getEntity().isValid() && bot.getEntity().getLocation().getY() < voidY) {
                if (blueCage != null) {
                    bot.spawnEntity(blueCage.clone().add(0.5, 0.5, 0.5), "BRIDGE");
                }
            }
        }
    }

    private void respawnPlayer(Player player) {
        if (state != GameState.IN_GAME) return;
        boolean isRed = redTeam.contains(player.getUniqueId());
        Location cage = isRed ? redCage : blueCage;
        if (cage == null) return;

        player.setFallDistance(0);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.getInventory().clear();
        giveKit(player, isRed);
        player.teleport(cage.clone().add(0.5, 0.5, 0.5));

        // Spawn protective team cage around player
        spawnCage(cage, isRed ? Material.RED_STAINED_GLASS : Material.BLUE_STAINED_GLASS);
        ChatUtil.sendTitle(player, "§cRESPAWNING...", "§eCage opens in 2s", 0, 30, 0);
        ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        // Release cage after 2 seconds (40 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state == GameState.IN_GAME && player.isOnline()) {
                removeCageAt(cage);
                ChatUtil.sendTitle(player, "§a§lRELEASED!", "§7Cross the bridge!", 0, 20, 5);
            }
        }, 40L);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (state != GameState.IN_GAME) return;
        Player p = event.getPlayer();
        if (!contains(p)) return;

        if (inRoundCountdown) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ())) {
                event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
            return;
        }

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

        Location goalLoc = team.equals("RED") ? blueGoal : redGoal;
        if (goalLoc != null && goalLoc.getWorld() != null) {
            goalLoc.getWorld().strikeLightningEffect(goalLoc);
            spawnFirework(goalLoc.clone().add(0, 2, 0));
        }

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
        if (state != GameState.IN_GAME || inRoundCountdown) {
            event.setCancelled(true);
            return;
        }
        // ONLY allow breaking blocks that were placed by players during this match
        if (!rollbackEngine.isPlayerPlaced(event.getBlock().getLocation())) {
            event.setCancelled(true);
            ChatUtil.sendMessage(event.getPlayer(), "§cYou can only break blocks placed by players!");
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
        if (lobbySpawn != null && event.getBlockPlaced().getY() >= lobbySpawn.getBlockY() - 3) {
            event.setCancelled(true);
            ChatUtil.sendMessage(event.getPlayer(), "§cYou cannot build that high!");
            return;
        }
        rollbackEngine.recordPlace(event.getBlockPlaced());
    }

    @Override
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (state != GameState.IN_GAME || inRoundCountdown) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player killer) {
            if (victim.getHealth() - event.getFinalDamage() <= 0) {
                event.setCancelled(true);
                kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
                killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
                killer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 60, 1));
                ChatUtil.sendActionBar(killer, "§6§lKILL! §a+1 Golden Apple §d(Regen II)!");
                ChatUtil.playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.6f);
                broadcast("§c" + victim.getName() + " §7was slain by §a" + killer.getName() + "§7!");
                respawnPlayer(victim);
            }
        }
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().clear();
        event.setCancelled(true);
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
            killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
            killer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 60, 1));
            ChatUtil.sendActionBar(killer, "§6§lKILL! §a+1 Golden Apple §d(Regen II)!");
            ChatUtil.playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.6f);
        }
        respawnPlayer(victim);
    }

    @Override
    public void checkWinCondition() {
        if (state != GameState.IN_GAME) return;
        // In testing mode or multiplayer, check total alive players including bots
        if (getAlivePlayerCount() <= 1) {
            List<Player> alive = getBukkitPlayers();
            endGame(alive.isEmpty() ? null : alive.get(0));
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
        kills.clear();
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
            lines.add("§fKills: §a" + kills.getOrDefault(player.getUniqueId(), 0));
        } else {
            lines.add("§fGame: §eEnding");
        }
        lines.add(" ");
        lines.add("§emc.fpixel.nl");
        return lines;
    }

    public Location getBlueCage() { return blueCage; }
    public Location getBlueSpawn() { return blueSpawn; }
    public Location getRedCage() { return redCage; }
    public Location getRedSpawn() { return redSpawn; }
}
