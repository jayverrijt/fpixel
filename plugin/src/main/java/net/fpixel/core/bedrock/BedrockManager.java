package net.fpixel.core.bedrock;

import net.fpixel.FPixelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.UUID;

public class BedrockManager {
    private final FPixelPlugin plugin;
    private boolean floodgateEnabled = false;

    public BedrockManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            floodgateEnabled = true;
            plugin.getLogger().info("Floodgate detected! Bedrock cross-play features activated.");
        } else {
            plugin.getLogger().info("Floodgate not loaded; running in Java-only mode.");
        }
    }

    public boolean isBedrock(Player player) {
        if (!floodgateEnabled) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public FloodgatePlayer getFloodgatePlayer(Player player) {
        if (!isBedrock(player)) return null;
        try {
            return FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        } catch (Throwable ignored) {
            return null;
        }
    }

    public boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }
}
