package net.fpixel.core.testing;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bridge.BridgeGame;
import net.fpixel.games.pvp.PvPGame;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.games.sumo.SumoGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;

public class TestingManager implements Listener {
    private final FPixelPlugin plugin;
    private boolean testingMode = false;
    private final List<String> botNames = List.of(
            "§c[Bot] Steve",
            "§9[Bot] Alex",
            "§6[Bot] Notch",
            "§d[Bot] Herobrine",
            "§a[Bot] Techno",
            "§e[Bot] Dream"
    );
    private int botIndex = 0;

    public TestingManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean isTestingMode() {
        return testingMode;
    }

    public void setTestingMode(boolean testingMode) {
        this.testingMode = testingMode;
    }

    public SimulatedPlayer createBot() {
        String name = botNames.get(botIndex % botNames.size());
        botIndex++;
        return new SimulatedPlayer(name);
    }

    public void onPlayerJoinArena(Arena arena, Player player) {
        if (!testingMode) return;
        if (arena.getState() != GameState.WAITING && arena.getState() != GameState.STARTING) return;

        int needed = arena.getMinPlayers() - arena.getPlayerCount();
        if (needed > 0) {
            ChatUtil.sendMessage(player, "§a§l[TESTING MODE] §eAuto-generating §b" + needed + " §esimulated player(s) to start the game...");
            for (int i = 0; i < needed; i++) {
                SimulatedPlayer bot = createBot();
                arena.addSimulatedPlayer(bot);
            }
            arena.setCountdown(3);
            ChatUtil.sendMessage(player, "§a§l[TESTING MODE] §eCountdown set to 3 seconds for instant testing!");
        }
    }

    public void onArenaStart(Arena arena) {
        if (arena.getSimulatedPlayers().isEmpty()) return;

        String gameType = arena.getGameType().toUpperCase();

        if (arena instanceof SumoGame sumo) {
            for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
                Location s2 = sumo.getSpawn2();
                if (s2 != null) {
                    bot.spawnEntity(s2, "SUMO");
                }
            }
        } else if (arena instanceof PvPGame pvp) {
            for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
                Location s2 = pvp.getSpawn2();
                if (s2 != null) {
                    bot.spawnEntity(s2, "PVP");
                }
            }
        } else if (arena instanceof BridgeGame bridge) {
            for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
                Location s2 = bridge.getBlueCage() != null ? bridge.getBlueCage() : bridge.getBlueSpawn();
                if (s2 != null) {
                    bot.spawnEntity(s2, "BRIDGE");
                }
            }
        } else if (arena instanceof BedWarsGame bw) {
            var blueTeam = bw.getTeam("BLUE");
            for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
                if (blueTeam != null && blueTeam.getSpawnLocation() != null) {
                    blueTeam.addMember(bot.getUuid());
                    blueTeam.setBedAlive(true);
                    bot.spawnEntity(blueTeam.getSpawnLocation(), "BEDWARS");
                }
            }
        } else if (arena instanceof SkyWarsGame sw) {
            int idx = 1;
            for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
                if (idx < sw.getIslandSpawns().size()) {
                    bot.spawnEntity(sw.getIslandSpawns().get(idx), "SKYWARS");
                } else if (sw.getLobbySpawn() != null) {
                    bot.spawnEntity(sw.getLobbySpawn(), "SKYWARS");
                }
                idx++;
            }
        } else {
            // Parkour, TNT Run, etc.
            for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
                Location loc = arena.getLobbySpawn();
                if (loc != null) {
                    bot.spawnEntity(loc, gameType);
                }
            }
        }
    }

    public Arena findArenaByBot(Entity entity) {
        if (!entity.getScoreboardTags().contains("fpixel_bot")) return null;
        for (Arena arena : plugin.getGameManager().getArenas()) {
            for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
                if (bot.getEntity() != null && bot.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                    return arena;
                }
            }
        }
        return null;
    }

    public SimulatedPlayer findBotByEntity(Arena arena, Entity entity) {
        if (arena == null) return null;
        for (SimulatedPlayer bot : arena.getSimulatedPlayers().values()) {
            if (bot.getEntity() != null && bot.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return bot;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBotDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getScoreboardTags().contains("fpixel_bot")) return;

        Arena arena = findArenaByBot(entity);
        if (arena == null || arena.getState() != GameState.IN_GAME) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Player damager) {
            ChatUtil.playSound(damager, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);
        }

        // For Sumo, negate damage so bot doesn't die from stick hits, but receives full knockback!
        if (arena.getGameType().equalsIgnoreCase("SUMO")) {
            event.setDamage(0.0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBotDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getScoreboardTags().contains("fpixel_bot")) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        Arena arena = findArenaByBot(entity);
        if (arena != null) {
            SimulatedPlayer bot = findBotByEntity(arena, entity);
            if (bot != null) {
                if (arena instanceof BridgeGame bridge) {
                    // In Bridge, bots respawn just like players instead of being eliminated
                    Location cage = bridge.getBlueCage();
                    if (cage != null) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (bridge.getState() == GameState.IN_GAME) {
                                bot.spawnEntity(cage.clone().add(0.5, 0.5, 0.5), "BRIDGE");
                            }
                        }, 40L);
                    }
                } else {
                    arena.eliminateSimulatedPlayer(bot);
                }
            }
        }
    }
}
