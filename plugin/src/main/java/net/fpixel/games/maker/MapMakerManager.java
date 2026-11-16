package net.fpixel.games.maker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fpixel.FPixelPlugin;
import net.fpixel.core.map.VoidChunkGenerator;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bridge.BridgeGame;
import net.fpixel.games.murdermystery.MurderMysteryGame;
import net.fpixel.games.parkour.ParkourGame;
import net.fpixel.games.prophunt.PropHuntGame;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MapMakerManager implements Listener {
    private final FPixelPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File mapsFile;
    private final Map<String, CustomMinigameMap> maps = new ConcurrentHashMap<>();

    public MapMakerManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        this.mapsFile = new File(plugin.getDataFolder(), "custom_maps.json");
        loadMaps();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public synchronized void loadMaps() {
        if (!mapsFile.exists()) return;
        try (FileReader reader = new FileReader(mapsFile)) {
            Type type = new TypeToken<Map<String, CustomMinigameMap>>() {}.getType();
            Map<String, CustomMinigameMap> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                maps.clear();
                maps.putAll(loaded);
                registerAllApprovedMaps();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load custom_maps.json: " + e.getMessage());
        }
    }

    public synchronized void saveMaps() {
        try {
            if (!mapsFile.getParentFile().exists()) mapsFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(mapsFile)) {
                gson.toJson(maps, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save custom_maps.json: " + e.getMessage());
        }
    }

    public Collection<CustomMinigameMap> getAllMaps() {
        return Collections.unmodifiableCollection(maps.values());
    }

    public CustomMinigameMap getMap(String id) {
        if (id == null) return null;
        return maps.get(id.toLowerCase());
    }

    public List<CustomMinigameMap> getMapsByCreator(String creatorName) {
        List<CustomMinigameMap> list = new ArrayList<>();
        if (creatorName == null) return list;
        String clean = creatorName.toLowerCase().startsWith(".") ? creatorName.toLowerCase().substring(1) : creatorName.toLowerCase();
        for (CustomMinigameMap map : maps.values()) {
            if (map.getCreatorName() != null) {
                String c = map.getCreatorName().toLowerCase();
                String cClean = c.startsWith(".") ? c.substring(1) : c;
                if (c.equals(creatorName.toLowerCase()) || cClean.equals(clean)) {
                    list.add(map);
                }
            }
        }
        return list;
    }

    public List<CustomMinigameMap> getReviewQueue() {
        List<CustomMinigameMap> list = new ArrayList<>();
        for (CustomMinigameMap m : maps.values()) {
            if (m.getStatus() == CustomMinigameMap.Status.PENDING_REVIEW) {
                list.add(m);
            }
        }
        return list;
    }

    public CustomMinigameMap createMap(String id, String name, CustomMinigameMap.GameType gameType, int teamCount, Player creator) {
        return createMap(id, name, gameType, teamCount, creator, creator != null ? creator.getName() : "Console");
    }

    public CustomMinigameMap createMap(String id, String name, CustomMinigameMap.GameType gameType, int teamCount, Player creator, String creatorName) {
        if (id == null || id.isBlank()) {
            id = generateMapId(gameType);
        } else {
            id = id.toLowerCase().trim().replace(" ", "_");
        }

        String finalName = (name != null && !name.isBlank()) ? name.replace("_", " ") : ("Map " + id.toUpperCase());
        String finalCreatorName = creator != null ? creator.getName() : (creatorName != null && !creatorName.isBlank() ? creatorName : "Console");
        UUID creatorUUID = creator != null ? creator.getUniqueId() : null;
        if (creatorUUID == null && creatorName != null && !creatorName.equalsIgnoreCase("Console")) {
            try {
                org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(creatorName);
                if (off != null) creatorUUID = off.getUniqueId();
            } catch (Exception ignored) {}
        }

        CustomMinigameMap map = new CustomMinigameMap(
                id,
                finalName,
                gameType,
                creatorUUID,
                finalCreatorName,
                teamCount
        );

        maps.put(id, map);
        saveMaps();

        // Create or get void world
        World world = getOrCreateBuildWorld(map);

        // Generate base layout for BedWars / SkyWars
        if (gameType == CustomMinigameMap.GameType.BEDWARS || gameType == CustomMinigameMap.GameType.SKYWARS) {
            generateIslands(world, map);
        } else if (gameType == CustomMinigameMap.GameType.PARKOUR) {
            generateParkourPlatform(world, map);
        } else {
            generateGenericPlatform(world, map);
        }

        // Auto-generate high-altitude waiting area lounge at Y=100
        generateWaitingLobby(world, map);

        if (creator != null) {
            creator.teleport(new Location(world, 0.5, 65, 0.5, 0f, 0f));
            creator.setGameMode(GameMode.CREATIVE);
            creator.setAllowFlight(true);
            creator.setFlying(true);
            giveSetupWand(creator);
            ChatUtil.sendMessage(creator, "§a§l[Map Maker] §eJe bouwwereld voor §f" + map.getName() + " §eis aangemaakt!");
            ChatUtil.sendMessage(creator, "§7Gebruik de Setup Wand om spawns, bedden en kisten in te stellen.");
        }

        return map;
    }

    private String generateMapId(CustomMinigameMap.GameType type) {
        String prefix = switch (type) {
            case BEDWARS -> "BW";
            case SKYWARS -> "SW";
            case PARKOUR -> "PK";
            case TNTRUN -> "TR";
            case MURDER_MYSTERY -> "MM";
            case PROP_HUNT -> "PH";
            case THE_BRIDGE -> "BR";
            case SUMO -> "SM";
        };
        int num = 1000 + new Random().nextInt(9000);
        return (prefix + "-" + num).toLowerCase();
    }

    public World getOrCreateBuildWorld(CustomMinigameMap map) {
        String wName = map.getWorldName();
        World world = Bukkit.getWorld(wName);
        if (world == null) {
            WorldCreator wc = new WorldCreator(wName);
            wc.generator(new VoidChunkGenerator());
            wc.environment(World.Environment.NORMAL);
            world = wc.createWorld();
        }
        if (world != null) {
            world.setPVP(true);
            world.setDifficulty(Difficulty.EASY);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setTime(6000L);
            for (org.bukkit.entity.Entity e : world.getEntities()) {
                if (e instanceof org.bukkit.entity.Monster || e instanceof org.bukkit.entity.Slime || e instanceof org.bukkit.entity.Phantom || e instanceof org.bukkit.entity.Bat) {
                    e.remove();
                }
            }
            // Ensure waiting lobby exists in world (builds at Y=100 if missing)
            if (world.getBlockAt(0, 99, 0).getType().isAir()) {
                generateWaitingLobby(world, map);
            }
        }
        return world;
    }

    public boolean isBuilderWorld(World world) {
        if (world == null) return false;
        String name = world.getName().toLowerCase();
        if (name.startsWith("map_build_")) return true;
        for (CustomMinigameMap map : maps.values()) {
            if (map.getWorldName() != null && map.getWorldName().equalsIgnoreCase(world.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Auto-generates a floating, transparent waiting lounge high above the map at Y=100.
     * Features an 11x11 floor with sea lantern borders, light blue / cyan glass floor for viewing the islands below,
     * protective glass safety walls, and a central gold spawn block.
     */
    public void generateWaitingLobby(World world, CustomMinigameMap map) {
        if (world == null || map == null) return;
        int wy = 100;

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                // Floor at wy - 1 (Y=99)
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    world.getBlockAt(x, wy - 1, z).setType(Material.SEA_LANTERN, false);
                } else if (Math.abs(x) == 0 && Math.abs(z) == 0) {
                    world.getBlockAt(x, wy - 1, z).setType(Material.GOLD_BLOCK, false);
                } else if ((Math.abs(x) + Math.abs(z)) % 2 == 0) {
                    world.getBlockAt(x, wy - 1, z).setType(Material.CYAN_STAINED_GLASS, false);
                } else {
                    world.getBlockAt(x, wy - 1, z).setType(Material.LIGHT_BLUE_STAINED_GLASS, false);
                }

                // Protective walls (3 blocks high so players can safely look down without falling into the void)
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    for (int dy = 0; dy < 3; dy++) {
                        world.getBlockAt(x, wy + dy, z).setType(Material.GLASS, false);
                    }
                } else {
                    // Ensure clear air space inside the lounge
                    for (int dy = 0; dy < 4; dy++) {
                        world.getBlockAt(x, wy + dy, z).setType(Material.AIR, false);
                    }
                }
            }
        }

        Location lobbyLoc = new Location(world, 0.5, wy, 0.5, 0f, 0f);
        map.setWaitingLobby(formatLoc(lobbyLoc));
        saveMaps();
    }

    public boolean enterBuildMode(Player player, String id) {
        if (player == null || id == null) return false;
        String cleanId = id.toLowerCase().trim();
        if (cleanId.startsWith("map_build_")) {
            cleanId = cleanId.substring("map_build_".length());
        }

        loadMaps(); // Ensure latest state from disk
        CustomMinigameMap map = getMap(cleanId);
        if (map == null) {
            for (CustomMinigameMap m : maps.values()) {
                if (m.getId().equalsIgnoreCase(cleanId) || m.getName().equalsIgnoreCase(cleanId)) {
                    map = m;
                    break;
                }
            }
        }

        if (map == null) {
            ChatUtil.sendMessage(player, "§cMap niet gevonden met ID: §f" + id);
            return false;
        }

        String pName = player.getName().toLowerCase();
        String pClean = pName.startsWith(".") ? pName.substring(1) : pName;
        String cName = map.getCreatorName() != null ? map.getCreatorName().toLowerCase() : "";
        String cClean = cName.startsWith(".") ? cName.substring(1) : cName;
        boolean isCreator = pName.equals(cName) || pClean.equals(cClean)
                || (map.getCreatorUuid() != null && map.getCreatorUuid().equals(player.getUniqueId()));
        boolean isStaff = plugin.getRankManager().getRank(player).isStaff();

        if (!isCreator && !isStaff) {
            ChatUtil.sendMessage(player, "§cJe bent niet de maker van deze map en geen stafflid.");
            return false;
        }

        World world = getOrCreateBuildWorld(map);
        Location target = new Location(world, 0.5, 65, 0.5, 0f, 0f);
        if (map.getSpectatorSpawn() != null) {
            Location s = parseLoc(map.getSpectatorSpawn());
            if (s != null) target = s;
        }

        player.teleport(target);
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        giveSetupWand(player);
        ChatUtil.sendMessage(player, "§a§l[Map Maker] §eWelkom in de bouwwereld voor: §f" + map.getName() + " §7(" + map.getId() + ")");
        ChatUtil.sendMessage(player, "§7Gebruik de Setup Wand om markers in te stellen. Typ §b/lobby §7om terug te keren naar de Hub.");
        return true;
    }

    public synchronized void reloadMaps() {
        loadMaps();
    }

    private void generateParkourPlatform(World world, CustomMinigameMap map) {
        int cy = 64;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(x, cy, z).setType(Material.GOLD_BLOCK, false);
            }
        }
        world.getBlockAt(0, cy + 1, 0).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
        map.setSpectatorSpawn(formatLoc(new Location(world, 0.5, cy + 2, 0.5)));
        saveMaps();
    }

    private void generateGenericPlatform(World world, CustomMinigameMap map) {
        int cy = 64;
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.getBlockAt(x, cy, z).setType(Material.SMOOTH_QUARTZ, false);
            }
        }
        map.setSpectatorSpawn(formatLoc(new Location(world, 0.5, cy + 1, 0.5)));
        saveMaps();
    }

    private void generateIslands(World world, CustomMinigameMap map) {
        int cy = 64;
        // 1. Mid Island (10x10) centered at (0, 64, 0)
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.getBlockAt(x, cy - 1, z).setType(Material.DEEPSLATE_BRICKS, false);
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    world.getBlockAt(x, cy, z).setType(Material.SMOOTH_QUARTZ, false);
                } else {
                    world.getBlockAt(x, cy, z).setType(Material.STONE_BRICKS, false);
                }
            }
        }

        // Mid generators or chests
        map.getMidChests().clear();
        map.getMidGenerators().clear();
        if (map.getGameType() == CustomMinigameMap.GameType.BEDWARS) {
            world.getBlockAt(2, cy, 2).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(-2, cy, -2).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(-2, cy, 2).setType(Material.DIAMOND_BLOCK, false);
            world.getBlockAt(2, cy, -2).setType(Material.DIAMOND_BLOCK, false);
            map.getMidGenerators().add(formatLoc(new Location(world, 2.5, cy + 1, 2.5)));
            map.getMidGenerators().add(formatLoc(new Location(world, -2.5, cy + 1, -2.5)));
            map.getMidGenerators().add(formatLoc(new Location(world, -2.5, cy + 1, 2.5)));
            map.getMidGenerators().add(formatLoc(new Location(world, 2.5, cy + 1, -2.5)));
        } else {
            world.getBlockAt(2, cy + 1, 2).setType(Material.CHEST, false);
            world.getBlockAt(-2, cy + 1, -2).setType(Material.CHEST, false);
            world.getBlockAt(-2, cy + 1, 2).setType(Material.CHEST, false);
            world.getBlockAt(2, cy + 1, -2).setType(Material.CHEST, false);
            map.getMidChests().add(formatLoc(new Location(world, 2, cy + 1, 2)));
            map.getMidChests().add(formatLoc(new Location(world, -2, cy + 1, -2)));
            map.getMidChests().add(formatLoc(new Location(world, -2, cy + 1, 2)));
            map.getMidChests().add(formatLoc(new Location(world, 2, cy + 1, -2)));
            for (String mcLocStr : map.getMidChests()) {
                Location mc = parseLoc(mcLocStr);
                if (mc != null && mc.getBlock().getState() instanceof Chest chest) {
                    net.fpixel.games.skywars.SkyWarsGame.fillStaticMidChest(chest.getBlockInventory());
                }
            }
        }

        // 2. Base Islands in circle (radius 40)
        int teams = map.getTeamCount();
        map.getTeamSpawns().clear();
        map.getTeamBeds().clear();
        map.getTeamGenerators().clear();
        map.getIslandChests().clear();

        Material[] teamWool = new Material[]{
                Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL,
                Material.CYAN_WOOL, Material.WHITE_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL
        };

        for (int i = 0; i < teams; i++) {
            double angle = (2 * Math.PI * i) / teams;
            int bx = (int) Math.round(40 * Math.cos(angle));
            int bz = (int) Math.round(40 * Math.sin(angle));

            Material wool = teamWool[i % teamWool.length];

            // 5x5 Island
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    world.getBlockAt(bx + dx, cy - 1, bz + dz).setType(Material.STONE, false);
                    world.getBlockAt(bx + dx, cy, bz + dz).setType(Material.GRASS_BLOCK, false);
                }
            }

            // Center marker
            world.getBlockAt(bx, cy, bz).setType(wool, false);

            // Generator
            world.getBlockAt(bx - 1, cy, bz).setType(Material.IRON_BLOCK, false);
            map.getTeamGenerators().add(formatLoc(new Location(world, bx - 0.5, cy + 1, bz + 0.5)));

            // Bed or Chest
            if (map.getGameType() == CustomMinigameMap.GameType.BEDWARS) {
                world.getBlockAt(bx + 1, cy + 1, bz).setType(Material.RED_BED, false);
                map.getTeamBeds().add(formatLoc(new Location(world, bx + 1, cy + 1, bz)));
            } else {
                world.getBlockAt(bx + 1, cy + 1, bz).setType(Material.CHEST, false);
                map.getIslandChests().add(formatLoc(new Location(world, bx + 1, cy + 1, bz)));
                if (world.getBlockAt(bx + 1, cy + 1, bz).getState() instanceof Chest chest) {
                    net.fpixel.games.skywars.SkyWarsGame.fillStaticIslandChest(chest.getBlockInventory());
                }
            }

            // Spawn Location
            Location teamSpawn = new Location(world, bx + 0.5, cy + 1, bz + 0.5, (float) Math.toDegrees(angle) + 90, 0f);
            map.getTeamSpawns().add(formatLoc(teamSpawn));
        }

        map.setSpectatorSpawn(formatLoc(new Location(world, 0.5, cy + 10, 0.5, 0f, 90f)));
        saveMaps();
    }

    public void giveSetupWand(Player player) {
        ItemStack wand = new ItemBuilder(Material.BLAZE_ROD)
                .name("§6§lMap Setup Wand")
                .lore(
                        "§7Rechtsklik een blok om instellingen te koppelen.",
                        "§7Linksklik in de lucht om het setup menu te openen.",
                        "",
                        "§e▶ Beheer spawns, bedden, generators & kisten!"
                )
                .build();
        player.getInventory().addItem(wand);
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BLAZE_ROD) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().getDisplayName().contains("Map Setup Wand")) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Check if player is currently in a map build world
        String wName = player.getWorld().getName();
        if (!wName.startsWith("map_build_")) {
            ChatUtil.sendMessage(player, "§cJe bent niet in een map bouwwereld!");
            return;
        }

        String mapId = wName.substring("map_build_".length());
        CustomMinigameMap map = getMap(mapId);
        if (map == null) return;

        if (map.getStatus() == CustomMinigameMap.Status.APPROVED) {
            ChatUtil.sendMessage(player, "§cDeze map is al goedgekeurd en kan niet meer gewijzigd worden.");
            return;
        }

        if (event.getClickedBlock() != null) {
            Block b = event.getClickedBlock();
            ChatUtil.sendMessage(player, "§a[Setup Wand] §eGekozen blok: §f" + b.getType().name() + " §7op (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ")");
            ChatUtil.sendMessage(player, "§7Gebruik `/fpixel wand <spawns|beds|gens|chests>` om dit punt op te slaan.");
        }
    }

    public boolean testMap(String id, Player staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) {
            ChatUtil.sendMessage(staff, "§cMap niet gevonden: " + id);
            return false;
        }

        World world = getOrCreateBuildWorld(map);
        Location target = null;
        if (map.getTeamSpawns() != null && !map.getTeamSpawns().isEmpty()) {
            target = parseLoc(map.getTeamSpawns().get(0));
        }
        if (target == null && map.getParkourCheckpoints() != null && !map.getParkourCheckpoints().isEmpty()) {
            target = parseLoc(map.getParkourCheckpoints().get(0));
        }
        if (target == null && map.getSpectatorSpawn() != null) {
            target = parseLoc(map.getSpectatorSpawn());
        }
        if (target == null) {
            target = new Location(world, 0.5, 65, 0.5);
        }

        staff.teleport(target);
        staff.setGameMode(GameMode.SURVIVAL);
        staff.setHealth(20.0);
        staff.setFoodLevel(20);

        // Auto-fill empty chests in the test world for SkyWars playtesting
        if (map.getGameType() == CustomMinigameMap.GameType.SKYWARS) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Chest chest && chest.getBlockInventory().isEmpty()) {
                        double dist = Math.sqrt(Math.pow(chest.getX(), 2) + Math.pow(chest.getZ(), 2));
                        if (dist <= 22) {
                            net.fpixel.games.skywars.SkyWarsGame.fillStaticMidChest(chest.getBlockInventory());
                        } else {
                            net.fpixel.games.skywars.SkyWarsGame.fillStaticIslandChest(chest.getBlockInventory());
                        }
                    }
                }
            }
        }

        ChatUtil.sendMessage(staff, "§a§l[Play Test] §eJe playtest nu map: §f" + map.getName() + " §7(" + map.getGameType().getDisplayName() + ") - Maker: §b" + map.getCreatorName());
        ChatUtil.sendMessage(staff, "§7Modus: §aSurvival Playtest §7| Wissel met §e/gmc§7, §e/gms§7 of §e/gmsp");
        ChatUtil.sendMessage(staff, "§7Status: §f" + map.getStatus() + " §7| Keur goed via §a/fpixel map approve " + map.getId() + " §7of verlaat via §b/lobby");
        return true;
    }

    public boolean approveMap(String id, org.bukkit.command.CommandSender staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) return false;

        map.setStatus(CustomMinigameMap.Status.APPROVED);
        map.setRejectionReason(null);
        saveMaps();
        registerMapAsArena(map);

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§a§l[Map Review] §eMap §f" + map.getName() + " §e(" + map.getId() + ") is §aGOEDGEKEURD§e en direct toegevoegd aan het Game Compass!");
        }
        return true;
    }

    public boolean rejectMap(String id, String reason, org.bukkit.command.CommandSender staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) return false;

        map.setStatus(CustomMinigameMap.Status.REJECTED);
        map.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Voldoet niet aan de community kwaliteitsrichtlijnen.");
        saveMaps();
        unregisterMapArena(map);

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§c§l[Map Review] §eMap §f" + map.getName() + " §e(" + map.getId() + ") is §cAFGEKEURD§e. Reden: §f" + map.getRejectionReason());
        }
        return true;
    }

    public boolean disableMap(String id, org.bukkit.command.CommandSender staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) return false;

        map.setStatus(CustomMinigameMap.Status.ARCHIVED);
        saveMaps();
        unregisterMapArena(map);

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§e§l[Map Beheer] §eMap §f" + map.getName() + " §e(" + map.getId() + ") is §cuit circulatie gehaald§e (Gearchiveerd).");
        }
        return true;
    }

    public boolean enableMap(String id, org.bukkit.command.CommandSender staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) return false;

        map.setStatus(CustomMinigameMap.Status.APPROVED);
        map.setRejectionReason(null);
        saveMaps();
        registerMapAsArena(map);

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§a§l[Map Beheer] §eMap §f" + map.getName() + " §e(" + map.getId() + ") is §ageheractiveerd§e en weer in circulatie gebracht!");
        }
        return true;
    }

    public boolean renameMap(String id, String newName, org.bukkit.command.CommandSender staff) {
        if (id == null || newName == null || newName.isBlank()) return false;
        CustomMinigameMap map = getMap(id);
        if (map == null) {
            if (staff != null) ChatUtil.sendMessage(staff, "§cMap niet gevonden met ID: §f" + id);
            return false;
        }

        String oldName = map.getName();
        map.setName(newName.trim());
        saveMaps();

        if (map.getStatus() == CustomMinigameMap.Status.APPROVED) {
            unregisterMapArena(map);
            registerMapAsArena(map);
        }

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§a§l[Map Beheer] §eMap §f" + id + " §eis hernoemd van §7\"" + oldName + "\" §enaar §f\"" + map.getName() + "\"§e.");
        }
        return true;
    }

    public void registerMapAsArena(CustomMinigameMap map) {
        if (map == null || map.getStatus() != CustomMinigameMap.Status.APPROVED) return;

        World world = getOrCreateBuildWorld(map);
        if (world == null) return;

        String arenaId = "custom-" + map.getGameType().name().toLowerCase() + "-" + map.getId().toLowerCase();
        String displayName = map.getName() + " §7(Community)";

        Location center = new Location(world, 0.5, 65, 0.5);
        Location waitingLobby = new Location(world, 0.5, 100, 0.5);

        if (map.getWaitingLobby() != null) {
            Location wl = parseLoc(map.getWaitingLobby());
            if (wl != null) waitingLobby = wl;
        }

        Location spectatorSpawn = waitingLobby.clone().add(0, 5, 0);

        if (map.getSpectatorSpawn() != null) {
            Location s = parseLoc(map.getSpectatorSpawn());
            if (s != null) spectatorSpawn = s;
        }

        switch (map.getGameType()) {
            case BEDWARS -> {
                BedWarsGame bw = new BedWarsGame(plugin, arenaId, displayName, 2, Math.max(2, map.getTeamCount()) * 4);
                bw.setLobbySpawn(waitingLobby);
                bw.setSpectatorSpawn(spectatorSpawn);
                List<String> spawns = map.getTeamSpawns();
                List<String> beds = map.getTeamBeds();
                String[] teamKeys = {"RED", "BLUE", "GREEN", "YELLOW", "CYAN", "WHITE", "PINK", "GRAY"};
                for (int i = 0; i < Math.min(map.getTeamCount(), teamKeys.length); i++) {
                    var t = bw.getTeam(teamKeys[i]);
                    if (t != null) {
                        if (i < spawns.size()) {
                            Location sp = parseLoc(spawns.get(i));
                            if (sp != null) t.setSpawnLocation(sp);
                        }
                        if (i < beds.size()) {
                            Location bp = parseLoc(beds.get(i));
                            if (bp != null) t.setBedLocation(bp);
                        }
                    }
                }
                plugin.getGameManager().registerArena(bw);
            }
            case SKYWARS -> {
                SkyWarsGame sw = new SkyWarsGame(plugin, arenaId, displayName, 2, Math.max(2, map.getTeamCount()) * 2, 60);
                sw.setLobbySpawn(waitingLobby);
                sw.setSpectatorSpawn(spectatorSpawn);
                if (map.getTeamSpawns() != null) {
                    for (String spStr : map.getTeamSpawns()) {
                        Location sp = parseLoc(spStr);
                        if (sp != null) sw.addIslandSpawn(sp);
                    }
                }
                if (map.getMidChests() != null) {
                    for (String mcStr : map.getMidChests()) {
                        Location mc = parseLoc(mcStr);
                        if (mc != null) sw.addMidChest(mc);
                    }
                }
                if (map.getIslandChests() != null) {
                    for (String icStr : map.getIslandChests()) {
                        Location ic = parseLoc(icStr);
                        if (ic != null) sw.addIslandChest(ic);
                    }
                }
                if (map.getTeamBeds() != null) {
                    for (String bStr : map.getTeamBeds()) {
                        Location b = parseLoc(bStr);
                        if (b != null && b.getBlock().getType() == Material.CHEST) {
                            sw.addIslandChest(b);
                        }
                    }
                }
                plugin.getGameManager().registerArena(sw);
            }
            case THE_BRIDGE -> {
                BridgeGame bg = new BridgeGame(plugin, arenaId, displayName, 40);
                bg.setLobbySpawn(waitingLobby);
                bg.setSpectatorSpawn(spectatorSpawn);
                plugin.getGameManager().registerArena(bg);
            }
            case SUMO -> {
                SumoGame sg = new SumoGame(plugin, arenaId, displayName, 50);
                sg.setLobbySpawn(waitingLobby);
                sg.setSpectatorSpawn(spectatorSpawn);
                sg.setSpawn1(center.clone().add(-4, 1, 0));
                sg.setSpawn2(center.clone().add(4, 1, 0));
                plugin.getGameManager().registerArena(sg);
            }
            case TNTRUN -> {
                TNTRunGame tg = new TNTRunGame(plugin, arenaId, displayName, 2, 8, 30);
                tg.setLobbySpawn(waitingLobby);
                tg.setSpectatorSpawn(spectatorSpawn);
                tg.setArenaSpawn(center.clone().add(0, 1, 0));
                plugin.getGameManager().registerArena(tg);
            }
            case MURDER_MYSTERY -> {
                MurderMysteryGame mm = new MurderMysteryGame(plugin, arenaId, displayName);
                mm.setLobbySpawn(waitingLobby);
                mm.setSpectatorSpawn(spectatorSpawn);
                mm.addSpawnPoint(center.clone().add(0, 1, 0));
                plugin.getGameManager().registerArena(mm);
            }
            case PROP_HUNT -> {
                PropHuntGame ph = new PropHuntGame(plugin, arenaId, displayName);
                ph.setLobbySpawn(waitingLobby);
                ph.setSpectatorSpawn(spectatorSpawn);
                ph.setHunterSpawn(waitingLobby);
                ph.addPropSpawn(center.clone().add(0, 1, 0));
                plugin.getGameManager().registerArena(ph);
            }
            case PARKOUR -> {
                ParkourGame pg = new ParkourGame(plugin, arenaId, displayName, 1, 8, 40);
                pg.setLobbySpawn(waitingLobby);
                pg.setSpectatorSpawn(spectatorSpawn);
                pg.setStartLocation(center.clone().add(0, 1, 0));
                plugin.getGameManager().registerArena(pg);
            }
        }
        plugin.getLogger().info("Registered custom approved minigame map as active arena: " + arenaId + " (" + displayName + ")");
    }

    public void registerAllApprovedMaps() {
        for (CustomMinigameMap m : maps.values()) {
            if (m.getStatus() == CustomMinigameMap.Status.APPROVED) {
                registerMapAsArena(m);
            }
        }
    }

    public void unregisterMapArena(CustomMinigameMap map) {
        if (map == null) return;
        String arenaId = "custom-" + map.getGameType().name().toLowerCase() + "-" + map.getId().toLowerCase();
        plugin.getGameManager().unregisterArena(arenaId);
    }

    public String formatLoc(Location loc) {
        if (loc == null || loc.getWorld() == null) return "world,0,64,0,0,0";
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    public Location parseLoc(String str) {
        if (str == null || str.isBlank()) return null;
        String[] parts = str.split(",");
        if (parts.length < 4) return null;
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
        return new Location(w, x, y, z, yaw, pitch);
    }
}
