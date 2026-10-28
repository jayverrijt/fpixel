package net.fpixel.core.rank;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

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
    private final Map<String, Rank> nameRanks = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

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
                Rank rank = Rank.valueOf(ranksConfig.getString(key, "MEMBER"));
                if (key.contains("-")) {
                    playerRanks.put(UUID.fromString(key), rank);
                } else {
                    nameRanks.put(key.toLowerCase(), rank);
                }
            } catch (Exception ignored) {}
        }
    }

    public void saveRanks() {
        for (Map.Entry<UUID, Rank> entry : playerRanks.entrySet()) {
            ranksConfig.set(entry.getKey().toString(), entry.getValue().name());
        }
        for (Map.Entry<String, Rank> entry : nameRanks.entrySet()) {
            ranksConfig.set(entry.getKey(), entry.getValue().name());
        }
        try {
            ranksConfig.save(ranksFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save ranks.yml: " + e.getMessage());
        }
    }

    public Rank getRank(Player player) {
        if (player == null) return Rank.MEMBER;
        if (playerRanks.containsKey(player.getUniqueId())) {
            return playerRanks.get(player.getUniqueId());
        }
        String name = player.getName().toLowerCase();
        if (nameRanks.containsKey(name)) {
            return nameRanks.get(name);
        }
        if (name.startsWith(".") && nameRanks.containsKey(name.substring(1))) {
            return nameRanks.get(name.substring(1));
        }
        return Rank.MEMBER;
    }

    public Rank getRank(UUID uuid) {
        return playerRanks.getOrDefault(uuid, Rank.MEMBER);
    }

    public void setRank(Player player, Rank rank) {
        setRank(player.getUniqueId(), rank);
        nameRanks.put(player.getName().toLowerCase(), rank);
        saveRanks();
        updatePlayerDisplayName(player);
        applyPermissions(player);
    }

    public void setRank(UUID uuid, Rank rank) {
        playerRanks.put(uuid, rank);
        saveRanks();
        Player target = Bukkit.getPlayer(uuid);
        if (target != null) {
            updatePlayerDisplayName(target);
            applyPermissions(target);
        }
    }

    public void setRankByName(String name, Rank rank) {
        nameRanks.put(name.toLowerCase(), rank);
        saveRanks();
        Player target = Bukkit.getPlayerExact(name);
        if (target != null) {
            updatePlayerDisplayName(target);
            applyPermissions(target);
        }
    }

    public void updatePlayerDisplayName(Player player) {
        Rank rank = getRank(player);
        String name = rank.getPrefix() + rank.getColor() + player.getName();
        player.displayName(ChatUtil.component(name));
        player.playerListName(ChatUtil.component(name));
        if (plugin.getTablistManager() != null) {
            plugin.getTablistManager().updateAllPlayerTeams();
        }
    }

    /**
     * Applies dynamic permissions according to the player's Rank without requiring manual OP.
     * [OWNER] gets full OP and wildcard permissions.
     * [ADMIN] gets all administrative, building, and management permissions, MIN debugging and stop/restart commands.
     * [MOD] gets moderation, supervision, and teleportation permissions.
     * VIP+ / VIP / YOUTUBE get lobby flying and cosmetic skin features.
     * MEMBERS get standard player commands.
     */
    public void applyPermissions(Player player) {
        if (player == null || !player.isOnline()) return;

        // Clean up previous attachment if exists
        PermissionAttachment oldAttachment = attachments.remove(player.getUniqueId());
        if (oldAttachment != null) {
            try {
                player.removeAttachment(oldAttachment);
            } catch (Exception ignored) {}
        }

        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);

        Rank rank = getRank(player);

        if (rank == Rank.OWNER) {
            // Owner gets full operator status and all permissions
            if (!player.isOp()) {
                player.setOp(true);
            }
            attachment.setPermission("*", true);
            attachment.setPermission("fpixel.*", true);
            attachment.setPermission("fpixel.owner", true);
            attachment.setPermission("fpixel.admin", true);
            attachment.setPermission("fpixel.mod", true);
            attachment.setPermission("fpixel.fly", true);
            attachment.setPermission("fpixel.debug", true);
            attachment.setPermission("fpixel.backup", true);
            attachment.setPermission("fpixel.update", true);
            attachment.setPermission("spark", true);
            attachment.setPermission("spark.profiler", true);
            attachment.setPermission("minecraft.command.stop", true);
            attachment.setPermission("bukkit.command.stop", true);
            attachment.setPermission("bukkit.command.restart", true);
        } else {
            // Non-owners do not have OP; their power comes from their assigned Rank permissions
            if (player.isOp()) {
                player.setOp(false);
            }

            if (rank == Rank.ADMIN) {
                // Deny stop/restart, operator granting, and debugging
                attachment.setPermission("minecraft.command.stop", false);
                attachment.setPermission("minecraft.command.restart", false);
                attachment.setPermission("bukkit.command.stop", false);
                attachment.setPermission("bukkit.command.restart", false);
                attachment.setPermission("minecraft.command.op", false);
                attachment.setPermission("minecraft.command.deop", false);
                attachment.setPermission("bukkit.command.op.give", false);
                attachment.setPermission("bukkit.command.op.take", false);
                attachment.setPermission("minecraft.command.debug", false);
                attachment.setPermission("fpixel.owner", false);
                attachment.setPermission("fpixel.update", false);
                attachment.setPermission("fpixel.backup", false);
                attachment.setPermission("fpixel.testing", false);
                attachment.setPermission("fpixel.debug", false);
                attachment.setPermission("spark", false);
                attachment.setPermission("spark.profiler", false);

                // Grant all administrative, world building, and gameplay commands
                attachment.setPermission("fpixel.admin", true);
                attachment.setPermission("fpixel.mod", true);
                attachment.setPermission("fpixel.fly", true);
                attachment.setPermission("fpixel.worldedit", true);
                attachment.setPermission("fpixel.fill", true);
                attachment.setPermission("fpixel.empty", true);
                attachment.setPermission("fpixel.forcestart", true);
                attachment.setPermission("fpixel.maps", true);
                attachment.setPermission("fpixel.buildmaps", true);
                attachment.setPermission("fpixel.import", true);
                attachment.setPermission("fpixel.coins", true);
                attachment.setPermission("fpixel.waypoint.admin", true);

                // Vanilla/Paper commands for Admins
                attachment.setPermission("minecraft.command.gamemode", true);
                attachment.setPermission("minecraft.command.tp", true);
                attachment.setPermission("minecraft.command.teleport", true);
                attachment.setPermission("minecraft.command.give", true);
                attachment.setPermission("minecraft.command.time", true);
                attachment.setPermission("minecraft.command.weather", true);
                attachment.setPermission("minecraft.command.kick", true);
                attachment.setPermission("minecraft.command.ban", true);
                attachment.setPermission("minecraft.command.pardon", true);
                attachment.setPermission("minecraft.command.whitelist", true);
                attachment.setPermission("minecraft.command.kill", true);
                attachment.setPermission("minecraft.command.clear", true);
                attachment.setPermission("minecraft.command.effect", true);
                attachment.setPermission("minecraft.command.enchant", true);
                attachment.setPermission("minecraft.command.summon", true);
                attachment.setPermission("minecraft.command.setblock", true);
                attachment.setPermission("minecraft.command.fill", true);
                attachment.setPermission("minecraft.command.clone", true);
                attachment.setPermission("minecraft.command.locate", true);
                attachment.setPermission("minecraft.command.difficulty", true);
                attachment.setPermission("minecraft.command.spawnpoint", true);
                attachment.setPermission("minecraft.command.setworldspawn", true);
                attachment.setPermission("minecraft.command.xp", true);
                attachment.setPermission("minecraft.command.experience", true);
                attachment.setPermission("minecraft.command.title", true);
                attachment.setPermission("minecraft.command.particle", true);
                attachment.setPermission("minecraft.command.playsound", true);
                attachment.setPermission("minecraft.command.worldborder", true);
                attachment.setPermission("minecraft.command.spreadplayers", true);
                attachment.setPermission("minecraft.command.attribute", true);
                attachment.setPermission("minecraft.command.data", true);
                attachment.setPermission("minecraft.command.defaultgamemode", true);
                attachment.setPermission("minecraft.command.scoreboard", true);
                attachment.setPermission("minecraft.command.team", true);
                attachment.setPermission("minecraft.command.bossbar", true);
                attachment.setPermission("minecraft.command.recipe", true);
                attachment.setPermission("minecraft.command.advancement", true);
                attachment.setPermission("minecraft.command.tag", true);
                attachment.setPermission("minecraft.command.loot", true);
                attachment.setPermission("minecraft.command.item", true);
                attachment.setPermission("minecraft.command.ride", true);
                attachment.setPermission("minecraft.command.damage", true);
                attachment.setPermission("bukkit.command.gamemode", true);
                attachment.setPermission("bukkit.command.teleport", true);
                attachment.setPermission("bukkit.command.whitelist", true);
                attachment.setPermission("bukkit.command.plugins", true);
                attachment.setPermission("bukkit.command.version", true);
                attachment.setPermission("bukkit.command.help", true);

                // SkinsRestorer for Admins
                attachment.setPermission("skinsrestorer.command", true);
                attachment.setPermission("skinsrestorer.command.set", true);
                attachment.setPermission("skinsrestorer.command.set.other", true);
                attachment.setPermission("skinsrestorer.command.clear", true);
                attachment.setPermission("skinsrestorer.command.clear.other", true);
            } else if (rank == Rank.MOD) {
                // Mod gets moderation, teleportation and minigame force start
                attachment.setPermission("fpixel.mod", true);
                attachment.setPermission("fpixel.fly", true);
                attachment.setPermission("fpixel.forcestart", true);
                attachment.setPermission("minecraft.command.kick", true);
                attachment.setPermission("minecraft.command.tp", true);
                attachment.setPermission("minecraft.command.teleport", true);
                attachment.setPermission("minecraft.command.msg", true);
                attachment.setPermission("minecraft.command.tell", true);
                attachment.setPermission("skinsrestorer.command", true);
                attachment.setPermission("skinsrestorer.command.set", true);
            } else if (rank.getWeight() >= 40) {
                // VIP, VIP+, MVP, MVP+, YOUTUBE
                attachment.setPermission("fpixel.fly", true);
                attachment.setPermission("skinsrestorer.command", true);
                attachment.setPermission("skinsrestorer.command.set", true);
                attachment.setPermission("skinsrestorer.command.clear", true);
            } else {
                // MEMBER
                attachment.setPermission("skinsrestorer.command", true);
                attachment.setPermission("skinsrestorer.command.set", true);
                attachment.setPermission("skinsrestorer.command.clear", true);
            }
        }

        try {
            player.recalculatePermissions();
            player.updateCommands();
        } catch (Exception ignored) {}
    }

    public void removePermissions(Player player) {
        if (player == null) return;
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (Exception ignored) {}
        }
    }
}
