package net.fpixel;

import net.fpixel.commands.FPixelCommand;
import net.fpixel.commands.JoinCommand;
import net.fpixel.commands.LobbyCommand;
import net.fpixel.commands.RankCommand;
import net.fpixel.core.bedrock.BedrockManager;
import net.fpixel.core.rank.RankManager;
import net.fpixel.core.scoreboard.ScoreboardManager;
import net.fpixel.core.tablist.TablistManager;
import net.fpixel.core.user.UserManager;
import net.fpixel.game.GameManager;
import net.fpixel.game.spectator.SpectatorManager;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bedwars.BedWarsSpawner;
import net.fpixel.games.bedwars.BedWarsTeam;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import net.fpixel.lobby.LobbyManager;
import net.fpixel.lobby.listeners.DoubleJumpListener;
import net.fpixel.lobby.listeners.LaunchpadListener;
import net.fpixel.lobby.listeners.LobbyListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class FPixelPlugin extends JavaPlugin {
    private static FPixelPlugin instance;

    private UserManager userManager;
    private RankManager rankManager;
    private ScoreboardManager scoreboardManager;
    private TablistManager tablistManager;
    private BedrockManager bedrockManager;
    private SpectatorManager spectatorManager;
    private GameManager gameManager;
    private LobbyManager lobbyManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("==========================================");
        getLogger().info("       FPixel Network Core - Enabling     ");
        getLogger().info("==========================================");

        // 1. Initialize Core Managers
        this.userManager = new UserManager(this);
        this.rankManager = new RankManager(this);
        this.bedrockManager = new BedrockManager(this);
        this.spectatorManager = new SpectatorManager(this);
        this.gameManager = new GameManager(this);
        this.lobbyManager = new LobbyManager(this);

        // 2. Visual & Display Managers (Scoreboards & Tablist)
        this.scoreboardManager = new ScoreboardManager(this);
        this.tablistManager = new TablistManager(this);

        // 3. Register Lobby & Gameplay Listeners
        Bukkit.getPluginManager().registerEvents(new LobbyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DoubleJumpListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LaunchpadListener(this), this);

        // 4. Register Commands
        FPixelCommand fpixelCmd = new FPixelCommand(this);
        getCommand("fpixel").setExecutor(fpixelCmd);
        getCommand("fpixel").setTabCompleter(fpixelCmd);

        LobbyCommand lobbyCmd = new LobbyCommand(this);
        getCommand("lobby").setExecutor(lobbyCmd);

        JoinCommand joinCmd = new JoinCommand(this);
        getCommand("join").setExecutor(joinCmd);
        getCommand("join").setTabCompleter(joinCmd);

        RankCommand rankCmd = new RankCommand(this);
        getCommand("setrank").setExecutor(rankCmd);
        getCommand("setrank").setTabCompleter(rankCmd);

        // 5. Register Default Playable Arenas
        registerDefaultArenas();

        getLogger().info("FPixelCore has successfully loaded! Ready for players.");
    }

    private void registerDefaultArenas() {
        Bukkit.getScheduler().runTask(this, () -> {
            World defaultWorld = Bukkit.getWorlds().get(0);
            Location spawn = defaultWorld.getSpawnLocation();

            // Default BedWars 1v1 Arena
            BedWarsGame bw = new BedWarsGame(this, "bw-1", "BedWars 1v1", 2, 2);
            bw.setLobbySpawn(spawn.clone().add(100, 10, 0));
            bw.setSpectatorSpawn(spawn.clone().add(100, 25, 0));
            BedWarsTeam red = bw.getTeam("RED");
            if (red != null) {
                red.setSpawnLocation(spawn.clone().add(80, 5, 0));
                red.setBedLocation(spawn.clone().add(78, 5, 0));
            }
            BedWarsTeam blue = bw.getTeam("BLUE");
            if (blue != null) {
                blue.setSpawnLocation(spawn.clone().add(120, 5, 0));
                blue.setBedLocation(spawn.clone().add(122, 5, 0));
            }
            bw.addSpawner(new BedWarsSpawner(spawn.clone().add(80, 5, 2), BedWarsSpawner.SpawnerType.IRON));
            bw.addSpawner(new BedWarsSpawner(spawn.clone().add(120, 5, 2), BedWarsSpawner.SpawnerType.IRON));
            bw.addSpawner(new BedWarsSpawner(spawn.clone().add(100, 5, 0), BedWarsSpawner.SpawnerType.DIAMOND));
            gameManager.registerArena(bw);

            // Default TNT Run Arena
            TNTRunGame tnt = new TNTRunGame(this, "tnt-1", "TNT Run Classic", 2, 8, (int) spawn.getY() - 15);
            tnt.setLobbySpawn(spawn.clone().add(0, 15, 100));
            tnt.setSpectatorSpawn(spawn.clone().add(0, 25, 100));
            gameManager.registerArena(tnt);

            // Default Sumo Arena
            SumoGame sumo = new SumoGame(this, "sumo-1", "Sumo 1v1", (int) spawn.getY() - 5);
            sumo.setLobbySpawn(spawn.clone().add(-100, 10, 0));
            sumo.setSpectatorSpawn(spawn.clone().add(-100, 20, 0));
            sumo.setSpawn1(spawn.clone().add(-97, 5, 0));
            sumo.setSpawn2(spawn.clone().add(-103, 5, 0));
            gameManager.registerArena(sumo);
        });
    }

    @Override
    public void onDisable() {
        if (userManager != null) {
            userManager.saveAll();
        }
        if (rankManager != null) {
            rankManager.saveRanks();
        }
        if (gameManager != null) {
            for (var arena : gameManager.getArenas()) {
                arena.resetArena();
            }
        }
        getLogger().info("FPixelCore has safely shut down.");
    }

    public static FPixelPlugin getInstance() { return instance; }
    public UserManager getUserManager() { return userManager; }
    public RankManager getRankManager() { return rankManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public TablistManager getTablistManager() { return tablistManager; }
    public BedrockManager getBedrockManager() { return bedrockManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public GameManager getGameManager() { return gameManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
}
