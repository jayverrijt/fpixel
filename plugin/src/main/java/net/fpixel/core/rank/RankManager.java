package net.fpixel.core.rank;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankManager {
    private final FPixelPlugin plugin;
    private final File ranksFile;
    private FileConfiguration ranksConfig;
    private final Map<UUID, Rank> playerRanks = new HashMap<>();

    public RankManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        loadRanks();
    }

    public void loadRanks() {
        if (!ranksFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                ranksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create ranks.yml: " + e.getMessage());
            }
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        for (String key : ranksConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Rank rank = Rank.valueOf(ranksConfig.getString(key, "DEFAULT"));
                playerRanks.put(uuid, rank);
            } catch (Exception ignored) {}
        }
    }

    public void saveRanks() {
        for (Map.Entry<UUID, Rank> entry : playerRanks.entrySet()) {
            ranksConfig.set(entry.getKey().toString(), entry.getValue().name());
        }
        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save ranks.yml: " + e.getMessage());
        }
    }

    public Rank getRank(Player player) {
        return getRank(player.getUniqueId());
    }

    public Rank getRank(UUID uuid) {
        return playerRanks.getOrDefault(uuid, Rank.DEFAULT);
    }

    public void setRank(Player player, Rank rank) {
        setRank(player.getUniqueId(), rank);
        updatePlayerDisplayName(player);
    }

    public void setRank(UUID uuid, Rank rank) {
        playerRanks.put(uuid, rank);
        saveRanks();
    }

    public void updatePlayerDisplayName(Player player) {
        Rank rank = getRank(player);
        String name = rank.getPrefix() + rank.getColor() + player.getName();
        player.displayName(ChatUtil.component(name));
        player.playerListName(ChatUtil.component(name));
    }
}
