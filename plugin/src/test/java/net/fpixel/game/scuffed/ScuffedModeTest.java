package net.fpixel.game.scuffed;

import net.fpixel.FPixelPlugin;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScuffedModeTest {

    private static class DummyArena extends Arena {
        DummyArena(FPixelPlugin plugin, World world) {
            super(plugin, "scuffed-test", "Scuffed Test", "SKYWARS", 2, 8);
            this.lobbySpawn = new Location(world, 0, 64, 0);
            this.state = GameState.IN_GAME;
        }

        @Override public void onGameStart() {}
        @Override public void handleGameTick() {}
        @Override public void onGameEnd(Player winner) {}
        @Override public void onArenaReset() {}
        @Override public void checkWinCondition() {}
        @Override public List<String> getScoreboardLines(Player player) { return Collections.emptyList(); }
    }

    @Test
    void scuffedModeAssignsModifiersOnStartAndClearsOnDisableOrReset() {
        FPixelPlugin plugin = BukkitTestHarness.createStubPlugin();
        ScuffedModeManager manager = new ScuffedModeManager(plugin);
        World world = BukkitTestHarness.createWorld("scuffed_world");
        DummyArena arena = new DummyArena(plugin, world);

        assertFalse(manager.isEnabled());
        manager.onArenaStart(arena);
        assertTrue(manager.getActiveModifiers(arena).isEmpty(), "No modifiers when scuffed mode is off");

        manager.setEnabled(true);
        assertTrue(manager.isEnabled());

        manager.onArenaStart(arena);
        assertFalse(manager.getActiveModifiers(arena).isEmpty(), "Modifiers should be assigned when scuffed mode is on");

        manager.onArenaReset(arena);
        assertTrue(manager.getActiveModifiers(arena).isEmpty(), "Modifiers should clear on arena reset");

        manager.setEnabled(false);
        assertFalse(manager.isEnabled());
    }
}
