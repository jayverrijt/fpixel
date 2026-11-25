package net.fpixel.game;

import net.fpixel.FPixelPlugin;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerProtectionTest {

    private FPixelPlugin plugin;
    private GameManager gameManager;
    private World arenaWorld;
    private TestArena arena;
    private Player alivePlayer;
    private Player spectatorPlayer;

    private static class TestArena extends Arena {
        TestArena(FPixelPlugin plugin, String id, Location lobbySpawn) {
            super(plugin, id, id, "TEST", 2, 8);
            this.lobbySpawn = lobbySpawn;
            this.state = GameState.IN_GAME;
        }

        void addAlive(Player p) {
            players.add(p.getUniqueId());
        }

        void addSpectator(Player p) {
            spectators.add(p.getUniqueId());
        }

        @Override public void onGameStart() {}
        @Override public void handleGameTick() {}
        @Override public void onGameEnd(Player winner) {}
        @Override public void onArenaReset() {}
        @Override public void checkWinCondition() {}
        @Override public java.util.List<String> getScoreboardLines(Player player) {
            return java.util.Collections.emptyList();
        }
    }

    @BeforeEach
    void setUp() {
        plugin = BukkitTestHarness.createStubPlugin();
        gameManager = new GameManager(plugin);
        arenaWorld = BukkitTestHarness.createWorld("map_build_test_1");
        arena = new TestArena(plugin, "test-arena", new Location(arenaWorld, 0, 64, 0));
        gameManager.registerArena(arena);

        alivePlayer = BukkitTestHarness.createPlayer("AlivePlayer", UUID.randomUUID(), arenaWorld);
        spectatorPlayer = BukkitTestHarness.createPlayer("SpectatorPlayer", UUID.randomUUID(), arenaWorld);
        arena.addAlive(alivePlayer);
        arena.addSpectator(spectatorPlayer);
    }

    @Test
    void spectatorCannotHitInGamePlayer() {
        var ds = BukkitTestHarness.proxy(org.bukkit.damage.DamageSource.class, (m, args) -> null);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                spectatorPlayer, alivePlayer, EntityDamageEvent.DamageCause.ENTITY_ATTACK, ds, 2.0);
        gameManager.onDamage(event);

        assertTrue(event.isCancelled(), "Spectator melee hit on in-game player must be cancelled");
    }

    @Test
    void spectatorProjectileCannotHitInGamePlayer() {
        var ds = BukkitTestHarness.proxy(org.bukkit.damage.DamageSource.class, (m, args) -> null);
        Projectile arrow = BukkitTestHarness.createProjectile(spectatorPlayer, arenaWorld);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                arrow, alivePlayer, EntityDamageEvent.DamageCause.PROJECTILE, ds, 4.0);
        gameManager.onDamage(event);

        assertTrue(event.isCancelled(), "Spectator projectile hit on in-game player must be cancelled");
    }

    @Test
    void inGamePlayerCannotHitSpectator() {
        var ds = BukkitTestHarness.proxy(org.bukkit.damage.DamageSource.class, (m, args) -> null);
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                alivePlayer, spectatorPlayer, EntityDamageEvent.DamageCause.ENTITY_ATTACK, ds, 2.0);
        gameManager.onDamage(event);

        assertTrue(event.isCancelled(), "In-game player must not be able to hit spectators");
    }

    @Test
    void spectatorCannotBreakOrPlaceBlocksOrPickupItems() {
        Block block = BukkitTestHarness.createBlock(arenaWorld, 1, 64, 1, Material.STONE);
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, spectatorPlayer);
        gameManager.onBlockBreak(breakEvent);
        assertTrue(breakEvent.isCancelled(), "Spectator must not break blocks");

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                block, block.getState(), block, new ItemStack(Material.STONE), spectatorPlayer, true, EquipmentSlot.HAND);
        gameManager.onBlockPlace(placeEvent);
        assertTrue(placeEvent.isCancelled(), "Spectator must not place blocks");

        Item itemEntity = BukkitTestHarness.proxy(Item.class, (m, args) -> null);
        EntityPickupItemEvent pickupEvent = new EntityPickupItemEvent(spectatorPlayer, itemEntity, 0);
        gameManager.onPickupItem(pickupEvent);
        assertTrue(pickupEvent.isCancelled(), "Spectator must not pick up items");
    }

    @Test
    void paintingsInArenaWorldsCannotBeBrokenOrDamaged() {
        Hanging painting = BukkitTestHarness.createHanging(arenaWorld);

        var damageSource = BukkitTestHarness.proxy(org.bukkit.damage.DamageSource.class, (m, args) -> null);
        HangingBreakByEntityEvent breakEvent = new HangingBreakByEntityEvent(painting, alivePlayer, damageSource);
        gameManager.onHangingBreak(breakEvent);
        assertTrue(breakEvent.isCancelled(), "Paintings in arena worlds must not be breakable");

        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
                alivePlayer, painting, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damageSource, 1.0);
        gameManager.onDamage(damageEvent);
        assertTrue(damageEvent.isCancelled(), "Paintings/item frames in arena worlds must not take damage");
    }
}
