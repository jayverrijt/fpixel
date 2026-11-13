package net.fpixel.games.maker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fpixel.FPixelPlugin;
import net.fpixel.core.map.VoidChunkGenerator;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.core.util.ItemBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
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
        if (id == null || id.isBlank()) {
            id = generateMapId(gameType);
        } else {
            id = id.toLowerCase().trim().replace(" ", "_");
        }

        CustomMinigameMap map = new CustomMinigameMap(
                id,
                name != null && !name.isBlank() ? name : ("Map " + id.toUpperCase()),
                gameType,
                creator != null ? creator.getUniqueId() : null,
                creator != null ? creator.getName() : "Console",
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
        return world;
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
        }

        // 2. Base Islands in circle (radius 40)
        int teams = map.getTeamCount();
        map.getTeamSpawns().clear();
        map.getTeamBeds().clear();
        map.getTeamGenerators().clear();

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
                map.getTeamBeds().add(formatLoc(new Location(world, bx + 1, cy + 1, bz)));
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
        Location target = new Location(world, 0.5, 66, 0.5);
        if (map.getSpectatorSpawn() != null) {
            Location s = parseLoc(map.getSpectatorSpawn());
            if (s != null) target = s;
        }

        staff.teleport(target);
        staff.setGameMode(GameMode.SPECTATOR);
        ChatUtil.sendMessage(staff, "§a§l[Map Review] §eJe test nu map: §f" + map.getName() + " §7(" + map.getId() + ") - Maker: §b" + map.getCreatorName());
        ChatUtil.sendMessage(staff, "§7Status: §f" + map.getStatus() + " §7| Typ `/fpixel map approve " + map.getId() + "` om goed te keuren.");
        return true;
    }

    public boolean approveMap(String id, Player staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) return false;

        map.setStatus(CustomMinigameMap.Status.APPROVED);
        map.setRejectionReason(null);
        saveMaps();

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§a§l[Map Review] §eMap §f" + map.getName() + " §e(" + map.getId() + ") is §aGOEDGEKEURD§e!");
        }
        return true;
    }

    public boolean rejectMap(String id, String reason, Player staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) return false;

        map.setStatus(CustomMinigameMap.Status.REJECTED);
        map.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Voldoet niet aan de community kwaliteitsrichtlijnen.");
        saveMaps();

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§c§l[Map Review] §eMap §f" + map.getName() + " §e(" + map.getId() + ") is §cAFGEKEURD§e. Reden: §f" + map.getRejectionReason());
        }
        return true;
    }

    public boolean disableMap(String id, Player staff) {
        CustomMinigameMap map = getMap(id);
        if (map == null) return false;

        map.setStatus(CustomMinigameMap.Status.ARCHIVED);
        saveMaps();

        if (staff != null) {
            ChatUtil.sendMessage(staff, "§e§l[Map Review] §eMap §f" + map.getName() + " §e(" + map.getId() + ") is gedeactiveerd (uit circulatie gehaald).");
        }
        return true;
    }

    private String formatLoc(Location loc) {
        if (loc == null || loc.getWorld() == null) return "world,0,64,0,0,0";
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location parseLoc(String str) {
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
