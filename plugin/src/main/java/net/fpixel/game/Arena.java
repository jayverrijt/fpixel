package net.fpixel.game;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.rollback.RollbackEngine;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import net.fpixel.core.testing.SimulatedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.*;

public abstract class Arena {
    protected final FPixelPlugin plugin;
    protected final String id;
    protected final String displayName;
    protected final String gameType;
    protected Location lobbySpawn;
    protected Location spectatorSpawn;
    protected int minPlayers;
    protected int maxPlayers;

    protected GameState state = GameState.WAITING;
    protected final Set<UUID> players = new HashSet<>();
    protected final Set<UUID> spectators = new HashSet<>();
    protected final Map<UUID, SimulatedPlayer> simulatedPlayers = new HashMap<>();

    protected int countdown = 10;
    protected int gameTime = 0;
    protected int endCountdown = 10;
    protected final RollbackEngine rollbackEngine = new RollbackEngine();

    public Arena(FPixelPlugin plugin, String id, String displayName, String gameType, int minPlayers, int maxPlayers) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.gameType = gameType;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    public void tick() {
        if (state == GameState.STARTING) {
            handleCountdownTick();
        } else if (state == GameState.IN_GAME) {
            gameTime++;
            handleGameTick();
            checkWinCondition();
        } else if (state == GameState.ENDING) {
            handleEndingTick();
        }
    }

    public void forceStart() {
        if (state == GameState.WAITING || state == GameState.STARTING) {
            countdown = 3;
            state = GameState.STARTING;
            broadcast("§a§lSTART §8» §eGame countdown has been force-started!");
        }
    }

    protected void handleCountdownTick() {
        if (getPlayerCount() < minPlayers) {
            state = GameState.WAITING;
            countdown = 10;
            broadcast("§cNot enough players! Countdown cancelled.");
            for (Player p : getBukkitPlayers()) {
                p.setLevel(0);
                p.setExp(0);
            }
            return;
        }

        for (Player p : getBukkitPlayers()) {
            p.setLevel(countdown);
            p.setExp((float) countdown / 10.0f);
        }

        if (countdown == 10 || countdown == 5 || (countdown <= 3 && countdown > 0)) {
            String color = countdown <= 3 ? (countdown == 1 ? "§c" : "§e") : "§a";
            for (Player p : getBukkitPlayers()) {
                ChatUtil.sendTitle(p, color + countdown, "§7The game is starting soon!", 0, 25, 5);
                ChatUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, countdown == 1 ? 2.0f : 1.0f);
            }
            broadcast("§eThe game starts in " + color + countdown + " §esecond" + (countdown == 1 ? "" : "s") + "!");
        }

        if (countdown <= 0) {
            startGame();
        } else {
            countdown--;
        }
    }

    protected void handleEndingTick() {
        // Spawn fireworks in celebration
        if (endCountdown % 2 == 0) {
            for (Player p : getBukkitPlayers()) {
                spawnFirework(p.getLocation());
            }
        }

        if (endCountdown <= 0) {
            resetArena();
        } else {
            endCountdown--;
        }
    }

    public void startGame() {
        state = GameState.IN_GAME;
        gameTime = 0;
        for (Player p : getBukkitPlayers()) {
            p.setLevel(0);
            p.setExp(0);
            p.setGameMode(GameMode.SURVIVAL);
            ChatUtil.sendTitle(p, "§6§lFIGHT!", "§eGood luck and have fun!", 5, 30, 10);
            ChatUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        }
        onGameStart();
        if (plugin.getTestingManager() != null) {
            plugin.getTestingManager().onArenaStart(this);
        }
    }

    public void endGame(Player winner) {
        state = GameState.ENDING;
        endCountdown = 10;

        String winnerName = winner != null ? winner.getName() : "Nobody";
        broadcast("§6§m--------------------------------------------------");
        broadcast("§e§l              GAME OVER!");
        broadcast(" ");
        broadcast("§f Winner: §6§l" + winnerName);
        broadcast("§6§m--------------------------------------------------");

        for (Player p : getAllBukkitPlayers()) {
            if (winner != null && p.getUniqueId().equals(winner.getUniqueId())) {
                ChatUtil.sendTitle(p, "§6§lVICTORY!", "§eYou won the game!", 10, 60, 20);
                ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                FPixelUser u = plugin.getUserManager().getUser(p);
                u.addCoins(50);
                u.addExperience(100);
                ChatUtil.sendMessage(p, "§6+50 Coins §7for winning!");
            } else {
                ChatUtil.sendTitle(p, "§c§lGAME OVER", "§7Winner: §e" + winnerName, 10, 60, 20);
            }
        }
        onGameEnd(winner);
    }

    public void resetArena() {
        state = GameState.RESETTING;

        // Clean up simulated bot entities
        for (SimulatedPlayer bot : simulatedPlayers.values()) {
            bot.removeEntity();
        }
        simulatedPlayers.clear();

        // Teleport all players back to Hub
        for (Player p : getAllBukkitPlayers()) {
            plugin.getLobbyManager().teleportToLobby(p);
        }

        players.clear();
        spectators.clear();
        countdown = 10;
        gameTime = 0;
        endCountdown = 10;

        // Instant map rollback
        rollbackEngine.rollback();

        onArenaReset();
        state = GameState.WAITING;
    }

    public boolean join(Player player) {
        if (state == GameState.IN_GAME || state == GameState.ENDING || state == GameState.RESETTING) {
            // Join as spectator
            if (player.getWorld() != null && player.getWorld().getName().equalsIgnoreCase("survival")) {
                if (plugin.getSurvivalManager() != null && plugin.getSurvivalManager().getProfileManager() != null) {
                    plugin.getSurvivalManager().getProfileManager().saveProfile(player);
                }
            }
            spectators.add(player.getUniqueId());
            plugin.getSpectatorManager().makeSpectator(player, this);
            if (spectatorSpawn != null) {
                player.teleport(spectatorSpawn);
            }
            return true;
        }

        if (getPlayerCount() >= maxPlayers) {
            ChatUtil.sendMessage(player, "§cThis arena is full!");
            return false;
        }

        if (player.getWorld() != null && player.getWorld().getName().equalsIgnoreCase("survival")) {
            if (plugin.getSurvivalManager() != null && plugin.getSurvivalManager().getProfileManager() != null) {
                plugin.getSurvivalManager().getProfileManager().saveProfile(player);
            }
        }

        players.add(player.getUniqueId());
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);

        ItemStack lobbyBed = new net.fpixel.core.util.ItemBuilder(Material.RED_BED)
                .name("§c§lBack to Lobby §7(Right Click)")
                .lore("§7Right click to return to the Hub.")
                .build();
        player.getInventory().setItem(8, lobbyBed);

        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }

        broadcast("§7" + player.getName() + " §ejoined the game (§b" + getPlayerCount() + "§e/§b" + maxPlayers + "§e)!");

        if (getPlayerCount() >= minPlayers && state == GameState.WAITING) {
            state = GameState.STARTING;
            countdown = (minPlayers == 2 && maxPlayers == 2) ? 3 : 10;
            if (plugin.getTestingManager() != null && plugin.getTestingManager().isTestingMode()) {
                countdown = 3;
            }
        }

        onPlayerJoin(player);

        if (plugin.getTestingManager() != null && plugin.getTestingManager().isTestingMode()) {
            plugin.getTestingManager().onPlayerJoinArena(this, player);
        }
        return true;
    }

    public void leave(Player player) {
        players.remove(player.getUniqueId());
        spectators.remove(player.getUniqueId());
        plugin.getSpectatorManager().removeSpectator(player);

        broadcast("§7" + player.getName() + " §eleft the game.");
        onPlayerLeave(player);

        if (state == GameState.IN_GAME) {
            checkWinCondition();
        } else if (state == GameState.STARTING && getPlayerCount() < minPlayers) {
            state = GameState.WAITING;
            countdown = 10;
            broadcast("§cNot enough players! Countdown cancelled.");
        }
    }

    public void eliminatePlayer(Player player) {
        players.remove(player.getUniqueId());
        spectators.add(player.getUniqueId());
        player.setFallDistance(0);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        plugin.getSpectatorManager().makeSpectator(player, this);
        if (spectatorSpawn != null) {
            player.teleport(spectatorSpawn);
        } else if (lobbySpawn != null) {
            player.teleport(lobbySpawn.clone().add(0, 5, 0));
        }
        broadcast("§c" + player.getName() + " §7has been eliminated!");
        checkWinCondition();
    }

    public void addSimulatedPlayer(SimulatedPlayer bot) {
        if (getPlayerCount() >= maxPlayers) return;
        simulatedPlayers.put(bot.getUuid(), bot);
        broadcast("§7" + bot.getName() + " §ejoined the game (§b" + getPlayerCount() + "§e/§b" + maxPlayers + "§e)!");

        if (getPlayerCount() >= minPlayers && state == GameState.WAITING) {
            state = GameState.STARTING;
            countdown = 3;
        }
    }

    public void removeSimulatedPlayer(UUID uuid) {
        SimulatedPlayer bot = simulatedPlayers.remove(uuid);
        if (bot != null) {
            bot.removeEntity();
            broadcast("§7" + bot.getName() + " §eleft the game.");
            if (state == GameState.IN_GAME) {
                checkWinCondition();
            } else if (state == GameState.STARTING && getPlayerCount() < minPlayers) {
                state = GameState.WAITING;
                countdown = 10;
            }
        }
    }

    public void eliminateSimulatedPlayer(SimulatedPlayer bot) {
        simulatedPlayers.remove(bot.getUuid());
        bot.removeEntity();
        broadcast("§c" + bot.getName() + " §7has been eliminated!");
        checkWinCondition();
    }

    public Map<UUID, SimulatedPlayer> getSimulatedPlayers() {
        return simulatedPlayers;
    }

    public int getAlivePlayerCount() {
        return players.size() + simulatedPlayers.size();
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public void broadcast(String message) {
        for (Player p : getAllBukkitPlayers()) {
            ChatUtil.sendRawMessage(p, message);
        }
    }

    protected void spawnFirework(Location loc) {
        if (loc.getWorld() == null) return;
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.ORANGE, Color.RED)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    public List<Player> getBukkitPlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) list.add(p);
        }
        return list;
    }

    public List<Player> getAllBukkitPlayers() {
        List<Player> list = getBukkitPlayers();
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && !list.contains(p)) list.add(p);
        }
        return list;
    }

    public List<Player> getAlivePlayers() {
        return getBukkitPlayers();
    }

    // Abstract & Hook methods for specific games
    public abstract void onGameStart();
    public abstract void handleGameTick();
    public abstract void onGameEnd(Player winner);
    public abstract void onArenaReset();
    public abstract void checkWinCondition();
    public abstract List<String> getScoreboardLines(Player player);

    public void onPlayerJoin(Player player) {}
    public void onPlayerLeave(Player player) {}
    public void onBlockBreak(BlockBreakEvent event) {}
    public void onBlockPlace(BlockPlaceEvent event) {}
    public void onPlayerDamage(EntityDamageByEntityEvent event) {}
    public void onPlayerDeath(PlayerDeathEvent event) {}
    public void onPlayerMove(PlayerMoveEvent event) {}
    public void onPlayerInteract(PlayerInteractEvent event) {}
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {}

    // Getters and setters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getGameType() { return gameType; }
    public GameState getState() { return state; }
    public RollbackEngine getRollbackEngine() { return rollbackEngine; }
    public void setLobbySpawn(Location lobbySpawn) { this.lobbySpawn = lobbySpawn; }
    public void setSpectatorSpawn(Location spectatorSpawn) { this.spectatorSpawn = spectatorSpawn; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public int getPlayerCount() { return players.size() + simulatedPlayers.size(); }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean contains(Player player) {
        return players.contains(player.getUniqueId()) || spectators.contains(player.getUniqueId());
    }
}
