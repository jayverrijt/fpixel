package net.fpixel;

import net.fpixel.commands.*;
import net.fpixel.core.bedrock.BedrockManager;
import net.fpixel.core.map.WorldManager;
import net.fpixel.core.rank.ChatListener;
import net.fpixel.core.rank.RankManager;
import net.fpixel.core.scoreboard.ScoreboardManager;
import net.fpixel.core.tablist.TablistManager;
import net.fpixel.core.user.UserManager;
import net.fpixel.duels.DuelManager;
import net.fpixel.game.GameManager;
import net.fpixel.game.spectator.SpectatorManager;
import net.fpixel.lobby.LobbyManager;
import net.fpixel.lobby.listeners.DoubleJumpListener;
import net.fpixel.lobby.listeners.LaunchpadListener;
import net.fpixel.lobby.listeners.LobbyListener;
import net.fpixel.party.PartyManager;
import net.fpixel.survival.SurvivalListener;
import net.fpixel.survival.SurvivalManager;
import net.fpixel.core.chunk.FPixelChunkHandler;
import org.bukkit.Bukkit;
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
    private WorldManager worldManager;
    private DuelManager duelManager;
    private PartyManager partyManager;
    private SurvivalManager survivalManager;
    private net.fpixel.lobby.HubNPCManager hubNPCManager;
    private net.fpixel.lobby.HubLoreManager hubLoreManager;
    private net.fpixel.core.testing.TestingManager testingManager;
    private net.fpixel.core.security.HostnameListener hostnameListener;


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
        this.hubNPCManager = new net.fpixel.lobby.HubNPCManager(this);
        this.hubLoreManager = new net.fpixel.lobby.HubLoreManager(this);
        this.testingManager = new net.fpixel.core.testing.TestingManager(this);
        this.duelManager = new DuelManager(this);
        this.partyManager = new PartyManager(this);
        this.survivalManager = new SurvivalManager(this);
        this.survivalManager.init();

        // 2. Visual & Display Managers (Scoreboards & Tablist)
        this.scoreboardManager = new ScoreboardManager(this);
        this.tablistManager = new TablistManager(this);

        // 3. Register Lobby, Chat, Gameplay & Security Listeners
        this.hostnameListener = new net.fpixel.core.security.HostnameListener(this);
        Bukkit.getPluginManager().registerEvents(this.hostnameListener, this);
        Bukkit.getPluginManager().registerEvents(new LobbyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DoubleJumpListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LaunchpadListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SurvivalListener(this), this);


        // 4. Register Commands
        FPixelChunkHandler chunkHandler = new FPixelChunkHandler(this);
        FPixelCommand fpixelCmd = new FPixelCommand(this, chunkHandler);
        getCommand("fpixel").setExecutor(fpixelCmd);
        getCommand("fpixel").setTabCompleter(fpixelCmd);
        getCommand("fill").setExecutor(fpixelCmd);
        getCommand("fill").setTabCompleter(fpixelCmd);
        getCommand("empty").setExecutor(fpixelCmd);
        getCommand("empty").setTabCompleter(fpixelCmd);

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

        DuelsCommand duelsCmd = new DuelsCommand(this);
        getCommand("duels").setExecutor(duelsCmd);
        getCommand("duels").setTabCompleter(duelsCmd);

        PartyCommand partyCmd = new PartyCommand(this);
        getCommand("party").setExecutor(partyCmd);
        getCommand("party").setTabCompleter(partyCmd);

        ForceStartCommand forceStartCmd = new ForceStartCommand(this);
        getCommand("forcestart").setExecutor(forceStartCmd);

        FlyCommand flyCmd = new FlyCommand(this);
        getCommand("fly").setExecutor(flyCmd);

        SurvivalCommand survivalCmd = new SurvivalCommand(this);
        getCommand("survival").setExecutor(survivalCmd);

        TestingCommand testingCmd = new TestingCommand(this);
        getCommand("testing").setExecutor(testingCmd);
        getCommand("testing").setTabCompleter(testingCmd);

        ShopCommand shopCmd = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCmd);

        WebPassCommand webPassCmd = new WebPassCommand(this);
        getCommand("webpass").setExecutor(webPassCmd);

        getCommand("secrets").setExecutor(this.hubLoreManager);

        SurvivalBackupCommand backupCmd = new SurvivalBackupCommand(this);
        getCommand("survivalbackup").setExecutor(backupCmd);

        CoinsCommand coinsCmd = new CoinsCommand(this);
        getCommand("coins").setExecutor(coinsCmd);
        getCommand("coins").setTabCompleter(coinsCmd);
        getCommand("addcoins").setExecutor(coinsCmd);
        getCommand("addcoins").setTabCompleter(coinsCmd);


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
        if (survivalManager != null) {
            survivalManager.saveAllActiveProfiles();
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
    public WorldManager getWorldManager() { return worldManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public SurvivalManager getSurvivalManager() { return survivalManager; }
    public net.fpixel.lobby.HubNPCManager getHubNPCManager() { return hubNPCManager; }
    public net.fpixel.lobby.HubLoreManager getHubLoreManager() { return hubLoreManager; }
    public net.fpixel.core.testing.TestingManager getTestingManager() { return testingManager; }
    public net.fpixel.core.security.HostnameListener getHostnameListener() { return hostnameListener; }

    @Override
    public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new net.fpixel.core.map.VoidChunkGenerator();
    }
}


