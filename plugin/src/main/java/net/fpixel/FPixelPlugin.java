package net.fpixel;

import net.fpixel.commands.FPixelCommand;
import net.fpixel.commands.JoinCommand;
import net.fpixel.commands.LobbyCommand;
import net.fpixel.commands.MapsCommand;
import net.fpixel.commands.RankCommand;
import net.fpixel.core.bedrock.BedrockManager;
import net.fpixel.core.map.WorldManager;
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
    private net.fpixel.core.map.WorldManager worldManager;

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

        MapsCommand mapsCmd = new MapsCommand(this);
        getCommand("maps").setExecutor(mapsCmd);
        getCommand("maps").setTabCompleter(mapsCmd);

        // 5. Build Grand Hub and 30 Minigame Maps (10 per game)
        this.worldManager = new WorldManager(this);
        Bukkit.getScheduler().runTask(this, () -> {
            World defaultWorld = Bukkit.getWorlds().get(0);
            worldManager.generateAllMaps(defaultWorld);
        });

        getLogger().info("FPixelCore has successfully loaded! Ready for players.");
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
    public net.fpixel.core.map.WorldManager getWorldManager() { return worldManager; }
}
