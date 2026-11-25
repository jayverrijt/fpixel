package net.fpixel.core.watchdog;

import net.fpixel.FPixelPlugin;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WatchdogListenerTest {

    private static class RecordingWatchdogManager extends WatchdogManager {
        final List<String> penalizedReasons = new ArrayList<>();

        RecordingWatchdogManager(FPixelPlugin plugin) {
            super(plugin);
        }

        @Override
        public void penalize(Player player, int penaltyPoints, String reason) {
            penalizedReasons.add(reason);
        }
    }

    @Test
    void jeiCheatPermissionChannelIsNotFlagged() {
        FPixelPlugin plugin = BukkitTestHarness.createStubPlugin();
        RecordingWatchdogManager watchdog = new RecordingWatchdogManager(plugin);
        WatchdogListener listener = new WatchdogListener(plugin, watchdog);

        World world = BukkitTestHarness.createWorld("world");
        Player player = BukkitTestHarness.createPlayer("Amigapower2018", UUID.randomUUID(), world);

        listener.onChannelRegister(new PlayerRegisterChannelEvent(player, "jei:cheat_permission"));

        assertTrue(watchdog.penalizedReasons.isEmpty(),
                "Legitimate JEI channel must not trigger a Watchdog penalty");
    }

    @Test
    void hackClientChannelsAreStillFlagged() {
        FPixelPlugin plugin = BukkitTestHarness.createStubPlugin();
        RecordingWatchdogManager watchdog = new RecordingWatchdogManager(plugin);
        WatchdogListener listener = new WatchdogListener(plugin, watchdog);

        World world = BukkitTestHarness.createWorld("world");
        Player player = BukkitTestHarness.createPlayer("Cheater", UUID.randomUUID(), world);

        listener.onChannelRegister(new PlayerRegisterChannelEvent(player, "wurst:hack"));
        listener.onChannelRegister(new PlayerRegisterChannelEvent(player, "meteor:client"));

        assertEquals(2, watchdog.penalizedReasons.size(),
                "Known hack client channels should still be penalized");
    }
}
