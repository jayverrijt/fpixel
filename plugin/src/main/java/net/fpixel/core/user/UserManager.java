package net.fpixel.core.user;

import net.fpixel.FPixelPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {
    private final FPixelPlugin plugin;
    private final File userDir;
    private final Map<UUID, FPixelUser> userMap = new HashMap<>();

    public UserManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.userDir = new File(plugin.getDataFolder(), "userdata");
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
    }

    public FPixelUser getUser(Player player) {
        return getUser(player.getUniqueId(), player.getName());
    }

    public FPixelUser getUser(UUID uuid, String defaultName) {
        return userMap.computeIfAbsent(uuid, id -> loadUser(id, defaultName));
    }

    public FPixelUser getCachedUser(UUID uuid) {
        return userMap.get(uuid);
    }

    public FPixelUser loadUser(UUID uuid, String name) {
        File file = new File(userDir, uuid.toString() + ".yml");
        FPixelUser user = new FPixelUser(uuid, name);
        if (file.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            user.setName(cfg.getString("name", name));
            user.addCoins(cfg.getInt("coins", 100) - 100);
            user.setVisibility(cfg.getString("visibility", "ALL"));
            
            // BedWars
            for (int i = 0; i < cfg.getInt("stats.bedwars.wins", 0); i++) user.addBedwarsWin();
            for (int i = 0; i < cfg.getInt("stats.bedwars.losses", 0); i++) user.addBedwarsLoss();
            for (int i = 0; i < cfg.getInt("stats.bedwars.kills", 0); i++) user.addBedwarsKill();
            for (int i = 0; i < cfg.getInt("stats.bedwars.finalKills", 0); i++) user.addBedwarsFinalKill();
            for (int i = 0; i < cfg.getInt("stats.bedwars.bedsBroken", 0); i++) user.addBedwarsBedBroken();

            // TNT Run
            for (int i = 0; i < cfg.getInt("stats.tntrun.wins", 0); i++) user.addTntrunWin();
            for (int i = 0; i < cfg.getInt("stats.tntrun.losses", 0); i++) user.addTntrunLoss();

            // Sumo
            for (int i = 0; i < cfg.getInt("stats.sumo.wins", 0); i++) user.addSumoWin();
            for (int i = 0; i < cfg.getInt("stats.sumo.losses", 0); i++) user.addSumoLoss();

            // Murder Mystery
            for (int i = 0; i < cfg.getInt("stats.murdermystery.wins", 0); i++) user.addMurderMysteryWin();

            // Prop Hunt
            for (int i = 0; i < cfg.getInt("stats.prophunt.wins", 0); i++) user.addPropHuntWin();

            for (String s : cfg.getStringList("secrets")) {
                user.addDiscoveredSecret(s);
            }
        }
        return user;
    }

    public void saveUser(FPixelUser user) {
        File file = new File(userDir, user.getUuid().toString() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("name", user.getName());
        cfg.set("coins", user.getCoins());
        cfg.set("level", user.getLevel());
        cfg.set("experience", user.getExperience());
        cfg.set("visibility", user.getVisibility());

        cfg.set("stats.bedwars.wins", user.getBedwarsWins());
        cfg.set("stats.bedwars.losses", user.getBedwarsLosses());
        cfg.set("stats.bedwars.kills", user.getBedwarsKills());
        cfg.set("stats.bedwars.finalKills", user.getBedwarsFinalKills());
        cfg.set("stats.bedwars.bedsBroken", user.getBedwarsBedsBroken());

        cfg.set("stats.tntrun.wins", user.getTntrunWins());
        cfg.set("stats.tntrun.losses", user.getTntrunLosses());

        cfg.set("stats.sumo.wins", user.getSumoWins());
        cfg.set("stats.sumo.losses", user.getSumoLosses());
        cfg.set("stats.sumo.streak", user.getSumoStreak());

        cfg.set("stats.murdermystery.wins", user.getMurderMysteryWins());
        cfg.set("stats.prophunt.wins", user.getPropHuntWins());

        cfg.set("secrets", new java.util.ArrayList<>(user.getDiscoveredSecrets()));

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save user " + user.getUuid() + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (FPixelUser user : userMap.values()) {
            saveUser(user);
        }
    }

    public void unloadUser(UUID uuid) {
        FPixelUser user = userMap.remove(uuid);
        if (user != null) {
            saveUser(user);
        }
    }
}
