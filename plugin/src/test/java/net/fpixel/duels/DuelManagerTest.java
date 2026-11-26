package net.fpixel.duels;

import net.fpixel.FPixelPlugin;
import net.fpixel.game.Arena;
import net.fpixel.game.GameManager;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuelManagerTest {

    private static class StubArena extends Arena {
        StubArena(FPixelPlugin plugin, String id, String gameType, int minPlayers, int maxPlayers) {
            super(plugin, id, id, gameType, minPlayers, maxPlayers);
        }

        @Override public void onGameStart() {}
        @Override public void handleGameTick() {}
        @Override public void onGameEnd(Player winner) {}
        @Override public void onArenaReset() {}
        @Override public void checkWinCondition() {}
        @Override public List<String> getScoreboardLines(Player player) { return Collections.emptyList(); }
    }

    @Test
    void findDuelArenaSupportsPropHuntAndMurderMysteryWithoutTwoPlayerCap() throws Exception {
        FPixelPlugin plugin = BukkitTestHarness.createStubPlugin();
        GameManager gameManager = new GameManager(plugin);
        Field gmField = FPixelPlugin.class.getDeclaredField("gameManager");
        gmField.setAccessible(true);
        gmField.set(plugin, gameManager);

        World world = BukkitTestHarness.createWorld("arenas");
        StubArena propHunt = new StubArena(plugin, "ph-warehouse", "PROP_HUNT", 2, 12);
        StubArena murderMystery = new StubArena(plugin, "mm-manor", "MURDER_MYSTERY", 3, 12);
        gameManager.registerArena(propHunt);
        gameManager.registerArena(murderMystery);

        DuelManager duelManager = new DuelManager(plugin);
        assertSame(propHunt, duelManager.findDuelArena("PROP_HUNT"),
                "PropHunt arena should be available in /duel even with maxPlayers > 2");
        assertSame(murderMystery, duelManager.findDuelArena("MURDER_MYSTERY"),
                "MurderMystery arena should be available in /duel even with maxPlayers > 2");
    }
}
