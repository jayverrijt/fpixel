package net.fpixel.worlds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerWorldManager implements Listener {
    private final FPixelPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File worldsFile;
    private final File dataDir;
    private final File worldsContainerDir;
    private final Map<String, PlayerWorld> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, String> currentWorldSession = new ConcurrentHashMap<>();

    public PlayerWorldManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.worldsFile = new File(plugin.getDataFolder(), "player_worlds.json");
        this.dataDir = new File(plugin.getDataFolder(), "player_worlds_data");
        this.dataDir.mkdirs();

        // Dedicated directory for git-backed player worlds
        File serverDir = plugin.getServer().getWorldContainer();
        this.worldsContainerDir = new File(serverDir, "player_worlds");
        if (!worldsContainerDir.exists()) {
            worldsContainerDir.mkdirs();
        }

        initGitRepo();
        loadWorlds();
        initDefaultSmp();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void initGitRepo() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File gitDir = new File(worldsContainerDir, ".git");
                if (!gitDir.exists()) {
                    runGitCommand(worldsContainerDir, "init", "-b", "world_backups");
                    runGitCommand(worldsContainerDir, "config", "user.name", "FPixel Backups");
                    runGitCommand(worldsContainerDir, "config", "user.email", "backups@fpixel.nl");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not initialize git in player_worlds: " + e.getMessage());
            }
        });
    }

    private void runGitCommand(File workingDir, String... cmd) throws IOException, InterruptedException {
        List<String> fullCmd = new ArrayList<>();
        fullCmd.add("git");
        fullCmd.addAll(Arrays.asList(cmd));
        Process p = new ProcessBuilder(fullCmd).directory(workingDir).start();
        p.waitFor();
    }

    public synchronized void loadWorlds() {
        if (!worldsFile.exists()) return;
        try (FileReader reader = new FileReader(worldsFile)) {
            Type type = new TypeToken<Map<String, PlayerWorld>>() {}.getType();
            Map<String, PlayerWorld> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                worlds.clear();
                worlds.putAll(loaded);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player_worlds.json: " + e.getMessage());
        }
    }

    public synchronized void saveWorlds() {
        try {
            if (!worldsFile.getParentFile().exists()) worldsFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(worldsFile)) {
                gson.toJson(worlds, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player_worlds.json: " + e.getMessage());
        }
    }

    private void initDefaultSmp() {
        boolean smpFound = false;
        for (PlayerWorld pw : worlds.values()) {
            if (pw.isPrimarySmp() || "ORIGIN1".equalsIgnoreCase(pw.getUid())) {
                smpFound = true;
                break;
            }
        }

        if (!smpFound) {
            PlayerWorld smp = new PlayerWorld(
                    "ORIGIN1",
                    "Survival SMP (Origineel)",
                    null,
                    "Jay_V0",
                    0L,
                    false,
                    true
            );
            worlds.put("ORIGIN1", smp);
            saveWorlds();
            plugin.getLogger().info("Registered primary Survival SMP (ORIGIN1) linked to Jay_V0.");
        }
    }

    public Collection<PlayerWorld> getAllWorlds() {
        return Collections.unmodifiableCollection(worlds.values());
    }

    public PlayerWorld getWorld(String uid) {
        if (uid == null) return null;
        return worlds.get(uid.toUpperCase());
    }

    public List<PlayerWorld> getAccessibleWorlds(Player player) {
        List<PlayerWorld> list = new ArrayList<>();
        for (PlayerWorld pw : worlds.values()) {
            if (pw.canAccess(player)) {
                list.add(pw);
            }
        }
        return list;
    }

    public int getMaxWorldsForRank(Rank rank) {
        if (rank == null) return 1;
        return switch (rank) {
            case OWNER, ADMIN -> Integer.MAX_VALUE;
            case MODERATOR -> 5;
            case DONOR -> 3;
            case MEMBER -> 1;
        };
    }

    public int getOwnedWorldCount(String playerName) {
        if (playerName == null) return 0;
        int count = 0;
        String clean = playerName.toLowerCase().startsWith(".") ? playerName.toLowerCase().substring(1) : playerName.toLowerCase();
        for (PlayerWorld pw : worlds.values()) {
            if (pw.isPrimarySmp()) continue; // Do not count root SMP towards limit
            if (pw.getCreatorName() != null) {
                String c = pw.getCreatorName().toLowerCase();
                String cClean = c.startsWith(".") ? c.substring(1) : c;
                if (c.equals(playerName.toLowerCase()) || cClean.equals(clean)) {
                    count++;
                }
            }
        }
        return count;
    }

    public synchronized PlayerWorld createWorld(String uid, String name, String creatorName, UUID creatorUuid, long seed, boolean cheats) {
        if (uid == null) {
            uid = generateUid();
        } else {
            uid = uid.toUpperCase().trim();
        }

        PlayerWorld pw = new PlayerWorld(uid, name, creatorUuid, creatorName, seed, cheats, false);
        worlds.put(uid, pw);
        saveWorlds();

        // Ensure directories and load world
        loadDimensions(pw);
        return pw;
    }

    public synchronized boolean deleteWorld(String uid) {
        PlayerWorld pw = getWorld(uid);
        if (pw == null || pw.isPrimarySmp()) return false;

        // Teleport any players inside to lobby
        unloadDimensions(pw, true);
        worlds.remove(uid.toUpperCase());
        saveWorlds();
        return true;
    }

    public String generateUid() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        for (int i = 0; i < 1000; i++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < 7; c++) {
                sb.append(chars.charAt(r.nextInt(chars.length())));
            }
            String uid = sb.toString();
            if (!worlds.containsKey(uid)) {
                return uid;
            }
        }
        return "W" + System.currentTimeMillis() % 1000000;
    }

    public World getOverworld(PlayerWorld pw) {
        if (pw.isPrimarySmp()) {
            World w = Bukkit.getWorld("survival");
            if (w != null) return w;
            return Bukkit.getWorld("world");
        }
        String path = "player_worlds/" + pw.getUid();
        World w = Bukkit.getWorld(path);
        if (w == null) {
            w = new WorldCreator(path)
                    .environment(World.Environment.NORMAL)
                    .seed(pw.getSeed() != 0 ? pw.getSeed() : new Random().nextLong())
                    .createWorld();
        }
        return w;
    }

    public World getNether(PlayerWorld pw) {
        if (pw.isPrimarySmp()) {
            World w = Bukkit.getWorld("world_nether");
            return w != null ? w : Bukkit.getWorld("the_nether");
        }
        String path = "player_worlds/" + pw.getUid() + "_nether";
        World w = Bukkit.getWorld(path);
        if (w == null) {
            w = new WorldCreator(path)
                    .environment(World.Environment.NETHER)
                    .createWorld();
        }
        return w;
    }

    public World getEnd(PlayerWorld pw) {
        if (pw.isPrimarySmp()) {
            World w = Bukkit.getWorld("world_the_end");
            return w != null ? w : Bukkit.getWorld("the_end");
        }
        String path = "player_worlds/" + pw.getUid() + "_the_end";
        World w = Bukkit.getWorld(path);
        if (w == null) {
            w = new WorldCreator(path)
                    .environment(World.Environment.THE_END)
                    .createWorld();
        }
        return w;
    }

    public void loadDimensions(PlayerWorld pw) {
        getOverworld(pw);
        getNether(pw);
        getEnd(pw);
    }

    public void unloadDimensions(PlayerWorld pw, boolean teleportPlayers) {
        if (pw.isPrimarySmp()) return;

        World overworld = Bukkit.getWorld("player_worlds/" + pw.getUid());
        World nether = Bukkit.getWorld("player_worlds/" + pw.getUid() + "_nether");
        World end = Bukkit.getWorld("player_worlds/" + pw.getUid() + "_the_end");

        Location lobbySpawn = plugin.getLobbyManager().getSpawnLocation();

        for (World w : Arrays.asList(overworld, nether, end)) {
            if (w != null) {
                if (teleportPlayers) {
                    for (Player p : w.getPlayers()) {
                        savePlayerState(p, pw.getUid());
                        p.teleport(lobbySpawn);
                        ChatUtil.sendMessage(p, "§e[Wereld] §6De wereld is ontladen. Je bent teruggestuurd naar de Hub.");
                    }
                }
                Bukkit.unloadWorld(w, true);
            }
        }
    }

    public boolean teleportPlayerToWorld(Player player, String uid) {
        PlayerWorld pw = getWorld(uid);
        if (pw == null) {
            ChatUtil.sendMessage(player, "§cWereld niet gevonden: " + uid);
            return false;
        }

        if (!pw.canAccess(player)) {
            ChatUtil.sendMessage(player, "§cJe hebt geen toegang tot wereld §f" + pw.getName() + " §7(" + pw.getUid() + ")§c!");
            return false;
        }

        // Save previous world state if needed
        String oldUid = currentWorldSession.get(player.getUniqueId());
        if (oldUid != null && !oldUid.equalsIgnoreCase(uid)) {
            savePlayerState(player, oldUid);
        }

        World targetWorld = getOverworld(pw);
        if (targetWorld == null) {
            ChatUtil.sendMessage(player, "§cKon wereld niet inladen. Probeer opnieuw.");
            return false;
        }

        Location spawn = targetWorld.getSpawnLocation().clone().add(0.5, 0.5, 0.5);
        player.teleport(spawn);

        currentWorldSession.put(player.getUniqueId(), pw.getUid());
        loadPlayerState(player, pw.getUid());

        ChatUtil.sendMessage(player, "§a§l[Wereld] §eWelkom in §f" + pw.getName() + "§e! (Eigenaar: §b" + pw.getCreatorName() + "§e)");
        if (pw.isCheatsEnabled()) {
            boolean canCheat = pw.canCheat(player);
            ChatUtil.sendMessage(player, canCheat ?
                    "§d[Cheats] §aCheats zijn INGESCHAKELD voor jou (/gamemode, /weather, /tp, /give, /time)." :
                    "§d[Cheats] §7Cheats zijn ingeschakeld op deze wereld, maar niet voor jouw account.");
        }
        return true;
    }

    public void openPlayerWorldsMenu(Player player) {
        new net.fpixel.worlds.menus.PlayerWorldsMenu(plugin, this, player).open();
    }

    // -------------------------------------------------------------
    // PER-WORLD INVENTORY & STATE PERSISTENCE
    // -------------------------------------------------------------
    public void savePlayerState(Player player, String worldUid) {
        if (player == null || worldUid == null) return;
        try {
            File dir = new File(dataDir, worldUid.toUpperCase());
            dir.mkdirs();
            File file = new File(dir, player.getUniqueId() + ".yml");
            YamlConfiguration cfg = new YamlConfiguration();

            cfg.set("inventory", player.getInventory().getContents());
            cfg.set("armor", player.getInventory().getArmorContents());
            cfg.set("offhand", player.getInventory().getItemInOffHand());
            cfg.set("enderchest", player.getEnderChest().getContents());
            cfg.set("exp", player.getExp());
            cfg.set("level", player.getLevel());
            cfg.set("health", player.getHealth());
            cfg.set("food", player.getFoodLevel());
            cfg.set("saturation", player.getSaturation());
            cfg.set("gamemode", player.getGameMode().name());

            cfg.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save player state for " + player.getName() + " in " + worldUid + ": " + e.getMessage());
        }
    }

    public void loadPlayerState(Player player, String worldUid) {
        if (player == null || worldUid == null) return;
        File file = new File(new File(dataDir, worldUid.toUpperCase()), player.getUniqueId() + ".yml");
        if (!file.exists()) {
            // First time in this world: clear inventory to prevent cheat leaks from other worlds/lobby
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(null);
            player.getEnderChest().clear();
            player.setExp(0);
            player.setLevel(0);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setGameMode(GameMode.SURVIVAL);
            return;
        }

        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            List<?> inv = cfg.getList("inventory");
            if (inv != null) {
                player.getInventory().setContents(inv.toArray(new ItemStack[0]));
            }
            List<?> armor = cfg.getList("armor");
            if (armor != null) {
                player.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));
            }
            ItemStack offhand = cfg.getItemStack("offhand");
            if (offhand != null) {
                player.getInventory().setItemInOffHand(offhand);
            }
            List<?> ec = cfg.getList("enderchest");
            if (ec != null) {
                player.getEnderChest().setContents(ec.toArray(new ItemStack[0]));
            }
            player.setExp((float) cfg.getDouble("exp", 0.0));
            player.setLevel(cfg.getInt("level", 0));
            double health = cfg.getDouble("health", 20.0);
            if (health > 0 && health <= 20) player.setHealth(health);
            player.setFoodLevel(cfg.getInt("food", 20));
            player.setSaturation((float) cfg.getDouble("saturation", 5.0));

            String gmStr = cfg.getString("gamemode", "SURVIVAL");
            try {
                player.setGameMode(GameMode.valueOf(gmStr));
            } catch (Exception ignored) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player state for " + player.getName() + " in " + worldUid + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // SCOPED CHEATS & EVENT LISTENERS
    // -------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        String[] parts = msg.split(" ");
        String baseCmd = parts[0].replace("/", "");

        Set<String> cheatCmds = Set.of("gamemode", "gm", "weather", "tp", "teleport", "give", "time");
        if (!cheatCmds.contains(baseCmd)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("fpixel.admin") || player.isOp()) {
            return; // Admins always allowed
        }

        PlayerWorld pw = getWorldForLocation(player.getLocation());
        if (pw == null || !pw.canCheat(player)) {
            event.setCancelled(true);
            ChatUtil.sendMessage(player, "§c[Cheats] Cheats zijn niet toegestaan in deze wereld voor jouw account.");
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorldName = event.getFrom().getName();
        String toWorldName = player.getWorld().getName();

        PlayerWorld fromPw = getWorldFromWorldName(fromWorldName);
        if (fromPw != null) {
            savePlayerState(player, fromPw.getUid());
        }

        PlayerWorld toPw = getWorldFromWorldName(toWorldName);
        if (toPw != null) {
            currentWorldSession.put(player.getUniqueId(), toPw.getUid());
            loadPlayerState(player, toPw.getUid());
        } else {
            // Player returned to Hub or Minigames
            currentWorldSession.remove(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            if (plugin.getLobbyManager().isLobbyWorld(player.getWorld())) {
                plugin.getLobbyManager().giveLobbyItems(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPortal(PlayerPortalEvent event) {
        PlayerWorld pw = getWorldForLocation(event.getFrom());
        if (pw == null) return;

        World currentWorld = event.getFrom().getWorld();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
            if (currentWorld.getEnvironment() == World.Environment.NETHER) {
                World ow = getOverworld(pw);
                if (ow != null) event.getPlayer().teleport(ow.getSpawnLocation());
            } else {
                World nether = getNether(pw);
                if (nether != null) event.getPlayer().teleport(nether.getSpawnLocation());
            }
        } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            if (currentWorld.getEnvironment() == World.Environment.THE_END) {
                World ow = getOverworld(pw);
                if (ow != null) event.getPlayer().teleport(ow.getSpawnLocation());
            } else {
                World end = getEnd(pw);
                if (end != null) event.getPlayer().teleport(new Location(end, 100.5, 49.0, 0.5));
            }
        }
    }

    public PlayerWorld getWorldForLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return getWorldFromWorldName(loc.getWorld().getName());
    }

    public PlayerWorld getWorldFromWorldName(String worldName) {
        if (worldName == null) return null;
        if (worldName.equalsIgnoreCase("survival") || worldName.equalsIgnoreCase("world") || worldName.equalsIgnoreCase("world_nether") || worldName.equalsIgnoreCase("world_the_end") || worldName.equalsIgnoreCase("the_nether") || worldName.equalsIgnoreCase("the_end")) {
            return worlds.get("ORIGIN1");
        }
        if (worldName.startsWith("player_worlds/")) {
            String sub = worldName.substring("player_worlds/".length());
            sub = sub.replace("_nether", "").replace("_the_end", "");
            return worlds.get(sub.toUpperCase());
        }
        return null;
    }

    // -------------------------------------------------------------
    // ZERO-DOWNTIME GIT BACKUPS & RESTORE
    // -------------------------------------------------------------
    public void createGitBackup(String uid, String requester, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        PlayerWorld pw = getWorld(uid);
        if (pw == null) {
            onError.accept("Wereld niet gevonden.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String dateStr = new SimpleDateFormat("yyyy+MM+dd+HHmm").format(new Date());
                String commitMsg = requester + "+" + pw.getName().replace(" ", "_") + "+" + dateStr;

                File repoDir = pw.isPrimarySmp() ? plugin.getServer().getWorldContainer() : worldsContainerDir;
                String targetPath = pw.isPrimarySmp() ? "world" : pw.getUid();

                runGitCommand(repoDir, "add", targetPath);
                runGitCommand(repoDir, "commit", "-m", commitMsg);

                if (onSuccess != null) {
                    Bukkit.getScheduler().runTask(plugin, onSuccess);
                }
            } catch (Exception e) {
                if (onError != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> onError.accept(e.getMessage()));
                }
            }
        });
    }

    public void restoreGitBackup(String uid, String commitHash, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        PlayerWorld pw = getWorld(uid);
        if (pw == null) {
            onError.accept("Wereld niet gevonden.");
            return;
        }

        // Must run sync for player teleport and world unloading
        Bukkit.getScheduler().runTask(plugin, () -> {
            unloadDimensions(pw, true);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    File repoDir = pw.isPrimarySmp() ? plugin.getServer().getWorldContainer() : worldsContainerDir;
                    String targetPath = pw.isPrimarySmp() ? "world" : pw.getUid();

                    runGitCommand(repoDir, "checkout", commitHash, "--", targetPath);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        loadDimensions(pw);
                        if (onSuccess != null) onSuccess.run();
                    });
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        loadDimensions(pw); // reload anyway
                        if (onError != null) onError.accept(e.getMessage());
                    });
                }
            });
        });
    }
}
