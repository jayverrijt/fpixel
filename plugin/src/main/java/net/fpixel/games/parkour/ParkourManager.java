package net.fpixel.games.parkour;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fpixel.FPixelPlugin;
import net.fpixel.core.map.MapBuilder;
import net.fpixel.core.map.VoidChunkGenerator;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.game.Arena;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.*;

public class ParkourManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ID_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // Alphanumeric without confusing chars

    private final FPixelPlugin plugin;
    private final File dataFile;
    private final Map<String, CustomParkourMap> maps = new LinkedHashMap<>();
    private final Map<UUID, ParkourTestSession> testSessions = new HashMap<>();
    private final Map<UUID, String> editingPlayers = new HashMap<>(); // player UUID -> mapId

    public ParkourManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "custom_parkour.json");
        loadMaps();
    }

    public synchronized void loadMaps() {
        maps.clear();
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            saveMaps();
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type listType = new TypeToken<List<CustomParkourMap>>(){}.getType();
            List<CustomParkourMap> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                for (CustomParkourMap map : loaded) {
                    if (map.getId() != null) {
                        maps.put(map.getId().toUpperCase(), map);
                    }
                }
            }
            plugin.getLogger().info("Loaded " + maps.size() + " custom parkour maps.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load custom_parkour.json: " + e.getMessage());
        }

        // Register all approved maps as active arenas
        registerAllApprovedArenas();
    }

    public synchronized void saveMaps() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try (FileWriter writer = new FileWriter(dataFile)) {
                GSON.toJson(new ArrayList<>(maps.values()), writer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save custom_parkour.json: " + e.getMessage());
        }
    }

    public World getOrCreateParkourWorld() {
        World world = Bukkit.getWorld("parkour_studio");
        if (world != null) return world;

        try {
            WorldCreator wc = new WorldCreator("parkour_studio");
            wc.generator(new VoidChunkGenerator());
            world = wc.createWorld();
            if (world != null) {
                world.setAutoSave(true);
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                world.setTime(6000L);
                world.setDifficulty(Difficulty.PEACEFUL);
            }
            return world;
        } catch (Exception e) {
            plugin.getLogger().severe("Could not create parkour_studio world: " + e.getMessage());
            return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
    }

    public String generateUniqueId() {
        return generateUniqueId(null);
    }

    public String generateUniqueId(String preferredName) {
        if (preferredName != null && !preferredName.trim().isEmpty()) {
            String slug = preferredName.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "");
            if (!slug.isEmpty()) {
                if (!maps.containsKey(slug)) {
                    return slug;
                }
                String pkSlug = "PK-" + slug;
                if (!maps.containsKey(pkSlug)) {
                    return pkSlug;
                }
                for (int i = 2; i <= 99; i++) {
                    String numbered = slug + "-" + i;
                    if (!maps.containsKey(numbered)) {
                        return numbered;
                    }
                }
            }
        }

        for (int attempts = 0; attempts < 1000; attempts++) {
            StringBuilder sb = new StringBuilder("PK-");
            for (int i = 0; i < 4; i++) {
                sb.append(ID_CHARS.charAt(RANDOM.nextInt(ID_CHARS.length())));
            }
            String candidate = sb.toString();
            if (!maps.containsKey(candidate)) {
                return candidate;
            }
        }
        return "PK-" + (System.currentTimeMillis() % 10000);
    }

    public int getNextSlotIndex() {
        int max = 0;
        for (CustomParkourMap m : maps.values()) {
            if (m.getSlotIndex() > max) {
                max = m.getSlotIndex();
            }
        }
        return max + 1;
    }

    /**
     * Creates a new parkour map in the void studio and teleports the creator.
     */
    public CustomParkourMap createMap(Player creator, String customName) {
        World world = getOrCreateParkourWorld();
        if (world == null) {
            ChatUtil.sendMessage(creator, "§cKon de parkour studio wereld niet laden!");
            return null;
        }

        String id = generateUniqueId(customName);
        String mapName;
        if (customName != null && !customName.trim().isEmpty()) {
            String trimmed = customName.trim();
            // Capitalize first letter if entered lowercase
            if (trimmed.length() > 1 && Character.isLowerCase(trimmed.charAt(0))) {
                mapName = Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
            } else {
                mapName = trimmed;
            }
        } else {
            mapName = "Parkour " + id;
        }

        int slot = getNextSlotIndex();
        int cx = slot * 500;
        int cy = 100;
        int cz = 0;

        // 1. Clear a 80x80x350 void cuboid area
        MapBuilder.fillBox(world, cx - 40, cy - 30, cz - 20, cx + 40, cy + 50, cz + 300, Material.AIR);

        // 2. Generate initial starting platform (5x5 polished andesite with stone bricks border)
        MapBuilder.fillCircle(world, cx, cy, cz, 3, Material.POLISHED_ANDESITE);
        MapBuilder.fillRing(world, cx, cy, cz, 2, 3, Material.STONE_BRICKS);
        world.getBlockAt(cx, cy, cz).setType(Material.POLISHED_ANDESITE, false);
        world.getBlockAt(cx, cy + 1, cz).setType(Material.STONE_PRESSURE_PLATE, false);

        // 3. Generate initial finish platform 40 blocks ahead as a clear design goal
        MapBuilder.fillCircle(world, cx, cy, cz + 40, 3, Material.QUARTZ_BLOCK);
        MapBuilder.fillRing(world, cx, cy, cz + 40, 2, 3, Material.SEA_LANTERN);
        world.getBlockAt(cx, cy, cz + 40).setType(Material.DIAMOND_BLOCK, false);
        world.getBlockAt(cx, cy + 1, cz + 40).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);

        CustomParkourMap map = new CustomParkourMap(id, mapName, creator.getName(), creator.getUniqueId(), slot);
        map.setWorldName(world.getName());
        map.setStart(cx + 0.5, cy + 1, cz + 0.5, 0f, 0f);
        map.setFinish(cx + 0.5, cy + 1, cz + 40 + 0.5);
        map.setVoidY(cy - 25);

        maps.put(id, map);
        saveMaps();

        // Teleport builder and prepare creative building session
        creator.teleport(new Location(world, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f));
        creator.setGameMode(GameMode.CREATIVE);
        creator.setAllowFlight(true);
        creator.setFlying(true);
        editingPlayers.put(creator.getUniqueId(), id);

        // Equipping builder toolset
        giveBuilderTools(creator, map);

        ChatUtil.sendMessage(creator, " ");
        ChatUtil.sendMessage(creator, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(creator, "§a§l          PARKOUR CREATOR STUDIO");
        ChatUtil.sendMessage(creator, "§7 Map ID: §e§l" + id + " §7(§f" + mapName + "§7)");
        ChatUtil.sendMessage(creator, "§7 Je bent geteleporteerd naar een leeg parkour canvas in de void!");
        ChatUtil.sendMessage(creator, "§7 Ontwerp je obstakels en sprongen vanaf dit startplatform.");
        ChatUtil.sendMessage(creator, " ");
        ChatUtil.sendMessage(creator, "§e§lBouw Instructies & Mechanica:");
        ChatUtil.sendMessage(creator, " §b• Stenen Drukplaat: §7Plaats voor §aCheckpoints§7 (geen boost).");
        ChatUtil.sendMessage(creator, " §6• Gouden Drukplaat: §6§lGiga Boost Pad 🚀§7.");
        ChatUtil.sendMessage(creator, " §2• Drukplaat op Emerald: §a§lJump Boost Pad ⚡§7.");
        ChatUtil.sendMessage(creator, " §e• Drukplaat op Gold: §e§lSpeed Boost Pad ⚡§7.");
        ChatUtil.sendMessage(creator, " §a• Slime Block op Emerald: §a§lBoost Slime Springboard§7.");
        ChatUtil.sendMessage(creator, " §7• Slime Block op normaal blok: §fStandaard Slime (normaal bouncen)§7.");
        ChatUtil.sendMessage(creator, " §c• IJzeren Drukplaat: §c§lFinish Lijn Marker 🏁§7.");
        ChatUtil.sendMessage(creator, " ");
        ChatUtil.sendMessage(creator, "§a• Commando's:");
        ChatUtil.sendMessage(creator, "    §e/fpixel parkour setstart §7- Verplaats startpunt");
        ChatUtil.sendMessage(creator, "    §e/fpixel parkour checkpoint §7- Voeg checkpoint toe");
        ChatUtil.sendMessage(creator, "    §e/fpixel parkour setfinish §7- Verplaats finishlijn");
        ChatUtil.sendMessage(creator, "    §e/fpixel test " + id + " §7- Test je parcours zelf");
        ChatUtil.sendMessage(creator, "    §e/hub §7- Terug naar de lobby");
        ChatUtil.sendMessage(creator, " ");
        ChatUtil.sendMessage(creator, "§7 Zodra je klaar bent kunnen §c[OWNER]s §7jouw map bekijken op de CMS");
        ChatUtil.sendMessage(creator, "§7 (§bmc.fpixel.nl§7) of testen via §e/fpixel test " + id + "§7!");
        ChatUtil.sendMessage(creator, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(creator, " ");

        ChatUtil.playSound(creator, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        return map;
    }

    public void giveBuilderTools(Player player, CustomParkourMap map) {
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemBuilder(Material.STONE_PRESSURE_PLATE)
                .name("§b§lCheckpoint Marker §7(Stenen Drukplaat)")
                .lore("§7Plaats op een neutraal blok voor een checkpoint.", "§8(Geeft géén boost op normale blokken)")
                .build());

        player.getInventory().setItem(1, new ItemBuilder(Material.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .name("§6§l🚀 Giga Boost Pad §7(Gouden Drukplaat)")
                .lore("§7Plaats deze gouden drukplaat om spelers", "§7een gigantische lancering / giga boost te geven!")
                .build());

        player.getInventory().setItem(2, new ItemBuilder(Material.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .name("§c§l🏁 Finish Lijn Marker §7(IJzeren Drukplaat)")
                .lore("§7Plaats deze drukplaat waar het parcours", "§7succesvol eindigt.")
                .build());

        player.getInventory().setItem(3, new ItemBuilder(Material.SLIME_BLOCK)
                .name("§a§lSlime Block")
                .lore("§7• Op neutraal blok: §fNormaal stuiteren (vanilla)", "§7• Op Emerald block: §aBoost Slime Springboard!")
                .build());

        player.getInventory().setItem(4, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("§2§l⚡ Jump Boost Basis §7(Emerald Block)")
                .lore("§7Plaats een drukplaat óf slime block hierop", "§7om een opwaartse Jump Boost te activeren!")
                .build());

        player.getInventory().setItem(5, new ItemBuilder(Material.GOLD_BLOCK)
                .name("§e§l⚡ Speed Boost Basis §7(Gold Block)")
                .lore("§7Plaats een drukplaat hierop voor Speed Boost,", "§7of gebruik als decoratief bouwblok.")
                .build());

        player.getInventory().setItem(6, new ItemBuilder(Material.POLISHED_ANDESITE)
                .name("§f§lNeutraal Bouwblok")
                .amount(64)
                .lore("§7Veilig neutraal blok voor platforms en checkpoints.")
                .build());

        player.getInventory().setItem(7, new ItemBuilder(Material.COMPASS)
                .name("§b§lTeleporteer naar Start §7(Rechtsklik)")
                .lore("§7Teleporteert je direct terug naar het startplatform.")
                .build());

        player.getInventory().setItem(8, new ItemBuilder(Material.RED_BED)
                .name("§c§lTerug naar Lobby §7(Rechtsklik)")
                .lore("§7Klik om terug te keren naar de Hub.", "§7Je bouwwerk blijft altijd opgeslagen.")
                .build());
    }

    /**
     * Start a review/testing session for a player on the specified map.
     */
    public boolean startTestingSession(Player player, String mapId) {
        CustomParkourMap map = getMap(mapId);
        if (map == null) {
            ChatUtil.sendMessage(player, "§cGeen parkour map gevonden met ID: §e" + mapId);
            return false;
        }

        World world = getOrCreateParkourWorld();
        Location startLoc = map.getStartLocation(world);
        if (startLoc == null) {
            ChatUtil.sendMessage(player, "§cStartlocatie voor deze map kon niet worden geladen!");
            return false;
        }

        // Leave any minigame arena if in one
        Arena currentArena = plugin.getGameManager().getPlayerArena(player);
        if (currentArena != null) {
            currentArena.leave(player);
        }

        // Create test session
        ParkourTestSession session = new ParkourTestSession(player, map);
        testSessions.put(player.getUniqueId(), session);

        // Setup player state
        player.teleport(startLoc.clone().add(0, 0.2, 0));
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        // Tester Hotbar
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemBuilder(Material.COMPASS)
                .name("§b§lHerstart Parcours §7(Rechtsklik)")
                .lore("§7Teleporteer terug naar het startpunt.")
                .build());

        Rank rank = plugin.getRankManager().getRank(player);
        if (rank != null && rank.getWeight() >= Rank.OWNER.getWeight()) {
            player.getInventory().setItem(7, new ItemBuilder(Material.EMERALD)
                    .name("§a§l✔ Keur Map Goed §7(Rechtsklik)")
                    .lore("§7Keur deze map direct goed voor", "§7de officiële Parkour games rotatie!")
                    .build());
        }

        player.getInventory().setItem(8, new ItemBuilder(Material.BARRIER)
                .name("§c§lStop Testen §7(Rechtsklik)")
                .lore("§7Verlaat testmodus en ga terug naar Lobby.")
                .build());

        ChatUtil.sendTitle(player, "§6§lPARKOUR REVIEW", "§eMap: §f" + map.getName(), 10, 40, 10);
        ChatUtil.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_0, 0.8f, 1.4f);

        ChatUtil.sendMessage(player, " ");
        ChatUtil.sendMessage(player, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(player, "§e§l         PARKOUR REVIEW & TEST");
        ChatUtil.sendMessage(player, "§7 Map: §f" + map.getName() + " §7(ID: §b" + map.getId() + "§7)");
        ChatUtil.sendMessage(player, "§7 Maker: §a" + map.getCreatorName());
        ChatUtil.sendMessage(player, "§7 Checkpoints: §e" + map.getCheckpoints().size());
        ChatUtil.sendMessage(player, "§7 Status: §f" + map.getStatus());
        ChatUtil.sendMessage(player, " ");
        ChatUtil.sendMessage(player, "§7 Test het parcours! Val je onder Y=" + map.getVoidY() + " dan respawn je bij je checkpoint.");
        if (rank != null && rank.getWeight() >= Rank.OWNER.getWeight()) {
            ChatUtil.sendMessage(player, "§a• Typ §e/fpixel parkour approve " + map.getId() + " §aom direct goed te keuren!");
            ChatUtil.sendMessage(player, "§c• Typ §e/fpixel parkour reject " + map.getId() + " [reden] §com af te wijzen.");
        }
        ChatUtil.sendMessage(player, "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendMessage(player, " ");
        return true;
    }

    public void stopTestingSession(Player player) {
        ParkourTestSession session = testSessions.remove(player.getUniqueId());
        if (session != null) {
            player.getInventory().clear();
            plugin.getLobbyManager().teleportToLobby(player);
            ChatUtil.sendMessage(player, "§aTestmodus beëindigd.");
        }
    }

    public ParkourTestSession getTestSession(Player player) {
        return player == null ? null : testSessions.get(player.getUniqueId());
    }

    public boolean isInTestSession(Player player) {
        return player != null && testSessions.containsKey(player.getUniqueId());
    }

    /**
     * Approves a map, marks it in custom_parkour.json, and registers the ParkourGame arena.
     */
    public boolean approveMap(CommandSender approver, String mapId) {
        CustomParkourMap map = getMap(mapId);
        if (map == null) {
            ChatUtil.sendMessage(approver, "§cGeen parkour map gevonden met ID: §e" + mapId);
            return false;
        }

        map.setStatus("APPROVED");
        map.setReviewer(approver.getName());
        map.setReviewedAt(System.currentTimeMillis());
        saveMaps();

        // Register arena into GameManager so it's instantly playable
        registerMapAsArena(map);

        // Server-wide celebration announcement
        Bukkit.broadcast(ChatUtil.component("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        Bukkit.broadcast(ChatUtil.component("§a§l       NIEUWE COMMUNITY PARKOUR MAP!"));
        Bukkit.broadcast(ChatUtil.component("§e Map §a\"" + map.getName() + "\" §7(ID: §b" + map.getId() + "§7) van §f" + map.getCreatorName()));
        Bukkit.broadcast(ChatUtil.component("§e is goedgekeurd door §c" + approver.getName() + " §een is nu §a§lSPEELBAAR§e!"));
        Bukkit.broadcast(ChatUtil.component("§7 Doe mee via §b/join parkour §7of de Game Selector NPC in de lobby!"));
        Bukkit.broadcast(ChatUtil.component("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            ChatUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        }

        return true;
    }

    /**
     * Rejects a map with an optional reason.
     */
    public boolean rejectMap(CommandSender rejecter, String mapId, String reason) {
        CustomParkourMap map = getMap(mapId);
        if (map == null) {
            ChatUtil.sendMessage(rejecter, "§cGeen parkour map gevonden met ID: §e" + mapId);
            return false;
        }

        map.setStatus("REJECTED");
        map.setReviewer(rejecter.getName());
        map.setReviewedAt(System.currentTimeMillis());
        map.setRejectReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Niet goedgekeurd door reviewer");
        saveMaps();

        ChatUtil.sendMessage(rejecter, "§cMap §e" + map.getName() + " (" + map.getId() + ") §cis gemarkeerd als afgewezen.");

        // If creator is online, let them know nicely
        Player creator = Bukkit.getPlayer(map.getCreatorUuid());
        if (creator != null && creator.isOnline()) {
            ChatUtil.sendMessage(creator, "§c§lPARKOUR §8» §7Jouw map §e" + map.getName() + " §7is beoordeeld:");
            ChatUtil.sendMessage(creator, "§cStatus: Afgewezen - §7" + map.getRejectReason());
            ChatUtil.sendMessage(creator, "§7Je kunt aanpassingen maken met §e/fpixel parkour build " + map.getId());
        }
        return true;
    }

    /**
     * Disables an approved map and takes it out of active circulation.
     */
    public boolean disableMap(CommandSender sender, String mapId) {
        CustomParkourMap map = getMap(mapId);
        if (map == null) {
            ChatUtil.sendMessage(sender, "§cGeen parkour map gevonden met ID: §e" + mapId);
            return false;
        }

        map.setStatus("DISABLED");
        saveMaps();

        String arenaId = "parkour-" + map.getId().toLowerCase().replace("-", "");
        plugin.getGameManager().unregisterArena(arenaId);

        ChatUtil.sendMessage(sender, "§6§lPARKOUR §8» §cMap §e\"" + map.getName() + "\" §7(" + map.getId() + ") §cis uit circulatie gehaald en niet meer speelbaar in games.");
        plugin.getLogger().info("Parkour map " + map.getId() + " disabled by " + sender.getName());
        return true;
    }

    /**
     * Registers an approved custom map as an active ParkourGame arena in GameManager.
     */
    public void registerMapAsArena(CustomParkourMap map) {
        World world = getOrCreateParkourWorld();
        if (world == null) return;

        String arenaId = "parkour-" + map.getId().toLowerCase().replace("-", "");
        String displayName = map.getName();

        // Check if already registered
        Arena existing = plugin.getGameManager().getArena(arenaId);
        if (existing != null) {
            return;
        }

        Location startLoc = map.getStartLocation(world);
        if (startLoc == null) {
            plugin.getLogger().warning("Could not register parkour arena " + arenaId + ": start location is null");
            return;
        }

        ParkourGame game = new ParkourGame(plugin, arenaId, displayName, 1, 16, map.getVoidY());
        game.setStartLocation(startLoc);

        // Build a floating glass waiting lobby 40 blocks above start platform
        Location lobbyLoc = startLoc.clone().add(0, 40, 0);
        MapBuilder.buildWaitingLobby(world, lobbyLoc, Material.SMOOTH_STONE, Material.OAK_FENCE, Material.LIME_STAINED_GLASS);
        game.setLobbySpawn(lobbyLoc.clone().add(0, 1, 0));
        game.setSpectatorSpawn(lobbyLoc.clone().add(0, 5, 0));

        // Add checkpoints
        for (Location cp : map.getCheckpointLocations(world)) {
            game.addCheckpoint(cp);
        }

        // Set finish
        Location finishLoc = map.getFinishLocation(world);
        if (finishLoc != null) {
            game.setFinishLocation(finishLoc);
        }

        plugin.getGameManager().registerArena(game);
        plugin.getLogger().info("Registered custom community parkour arena: " + arenaId + " (" + displayName + ")");
    }

    public void registerAllApprovedArenas() {
        for (CustomParkourMap map : maps.values()) {
            String arenaId = "parkour-" + map.getId().toLowerCase().replace("-", "");
            if (map.isApproved()) {
                registerMapAsArena(map);
            } else {
                plugin.getGameManager().unregisterArena(arenaId);
            }
        }
    }

    public CustomParkourMap getMap(String idOrName) {
        if (idOrName == null) return null;
        String clean = idOrName.trim();
        CustomParkourMap m = maps.get(clean.toUpperCase());
        if (m != null) return m;

        // Try lookup by exact name (case-insensitive)
        for (CustomParkourMap candidate : maps.values()) {
            if (candidate.getName() != null && candidate.getName().equalsIgnoreCase(clean)) {
                return candidate;
            }
        }

        // Try lookup with PK- prefix
        CustomParkourMap pk = maps.get("PK-" + clean.toUpperCase());
        if (pk != null) return pk;

        return null;
    }

    public Collection<CustomParkourMap> getAllMaps() {
        return maps.values();
    }

    public CustomParkourMap getMapBySlot(int slot) {
        for (CustomParkourMap m : maps.values()) {
            if (m.getSlotIndex() == slot) return m;
        }
        return null;
    }

    public String getEditingMapId(Player player) {
        return player == null ? null : editingPlayers.get(player.getUniqueId());
    }

    public void setEditingMapId(Player player, String mapId) {
        if (player == null) return;
        if (mapId == null) {
            editingPlayers.remove(player.getUniqueId());
        } else {
            editingPlayers.put(player.getUniqueId(), mapId);
        }
    }

    public void spawnCompletionFireworks(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc.clone().add(0, 1, 0), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.LIME, Color.YELLOW, Color.WHITE)
                .withFade(Color.AQUA)
                .withTrail()
                .withFlicker()
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
}
