package net.fpixel.lobby.portal;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.*;

public class HubPortalManager {
    public enum GamePortalType {
        PLAYER_WORLDS("§a§l🌍 PLAYER WORLDS", "§7Survival & SMP", "player_worlds"),
        PARKOUR("§6§l🏃 PARKOUR STUDIO", "§7Obstakels & Duels", "parkour"),
        BEDWARS("§c§l🛏️ BEDWARS", "§7Team Gevechten", "bedwars"),
        SKYWARS("§b§l👁️ SKYWARS", "§7Zwevende Eilanden", "skywars"),
        TNTRUN("§e§l🧨 TNT RUN", "§7Instortende Vloeren", "tntrun"),
        MURDER_MYSTERY("§d§l🗡️ MURDER MYSTERY", "§7Zoek de Moordenaar", "murdermystery"),
        PROP_HUNT("§3§l📦 PROP HUNT", "§7Blokken Verstoppertje", "prophunt"),
        THE_BRIDGE_SUMO("§5§l🌉 THE BRIDGE & SUMO", "§71v1 Snelle Duels", "duels");

        private final String title;
        private final String subtitle;
        private final String gameKey;

        GamePortalType(String title, String subtitle, String gameKey) {
            this.title = title;
            this.subtitle = subtitle;
            this.gameKey = gameKey;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getGameKey() { return gameKey; }
    }

    public static class PortalData {
        public final GamePortalType type;
        public final Location center;
        public final int minX, maxX, minY, maxY, minZ, maxZ;
        public TextDisplay hologram;

        public PortalData(GamePortalType type, Location center, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.type = type;
            this.center = center;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public boolean contains(Location loc) {
            if (loc == null || !loc.getWorld().equals(center.getWorld())) return false;
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    private final FPixelPlugin plugin;
    private final List<PortalData> portals = new ArrayList<>();
    private final Map<UUID, Long> lastTriggerTime = new HashMap<>();

    public HubPortalManager(FPixelPlugin plugin) {
        this.plugin = plugin;
        startHologramUpdater();
    }

    public void setupPortals(World world, int cy) {
        clearOldHolograms(world);
        portals.clear();

        // 8 Portals matching HubGenerator offsets
        addPortal(world, GamePortalType.PLAYER_WORLDS, 0, cy, -26, 2, 0);
        addPortal(world, GamePortalType.PARKOUR, 18, cy, -18, 2, 2);
        addPortal(world, GamePortalType.BEDWARS, 26, cy, 0, 0, 2);
        addPortal(world, GamePortalType.SKYWARS, 18, cy, 18, 2, 2);
        addPortal(world, GamePortalType.TNTRUN, 0, cy, 26, 2, 0);
        addPortal(world, GamePortalType.MURDER_MYSTERY, -18, cy, 18, 2, 2);
        addPortal(world, GamePortalType.PROP_HUNT, -26, cy, 0, 0, 2);
        addPortal(world, GamePortalType.THE_BRIDGE_SUMO, -18, cy, -18, 2, 2);

        spawnHolograms(world, cy);
    }

    private void addPortal(World world, GamePortalType type, int px, int cy, int pz, int spreadX, int spreadZ) {
        Location center = new Location(world, px + 0.5, cy + 3.0, pz + 0.5);
        int minX = px - Math.max(1, spreadX);
        int maxX = px + Math.max(1, spreadX) + 1;
        int minZ = pz - Math.max(1, spreadZ);
        int maxZ = pz + Math.max(1, spreadZ) + 1;
        int minY = cy;
        int maxY = cy + 7;

        portals.add(new PortalData(type, center, minX, maxX, minY, maxY, minZ, maxZ));
    }

    private void clearOldHolograms(World world) {
        for (TextDisplay td : world.getEntitiesByClass(TextDisplay.class)) {
            if (td.getScoreboardTags().contains("fpixel_portal_holo")) {
                td.remove();
            }
        }
    }

    private void spawnHolograms(World world, int cy) {
        for (PortalData portal : portals) {
            Location holoLoc = portal.center.clone().add(0, 3.8, 0);
            TextDisplay display = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
            display.addScoreboardTag("fpixel_portal_holo");
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setDefaultBackground(true);
            display.setShadowed(true);

            portal.hologram = display;
            updatePortalHologram(portal);
        }
    }

    public void updateAllHolograms() {
        for (PortalData portal : portals) {
            updatePortalHologram(portal);
        }
    }

    private void updatePortalHologram(PortalData portal) {
        if (portal.hologram == null || !portal.hologram.isValid()) return;

        int playerCount = getPlayerCountForMode(portal.type);
        String text = portal.type.getTitle() + "\n" +
                portal.type.getSubtitle() + "\n" +
                "§e● " + playerCount + " spelers online\n" +
                "§a[Klik of loop binnen]";

        portal.hologram.text(ChatUtil.component(text));
    }

    private int getPlayerCountForMode(GamePortalType type) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld() == null) continue;
            String wName = p.getWorld().getName().toLowerCase();
            switch (type) {
                case PLAYER_WORLDS:
                    if (wName.contains("survival") || wName.contains("player_worlds")) count++;
                    break;
                case PARKOUR:
                    if (wName.contains("parkour")) count++;
                    break;
                case BEDWARS:
                    if (wName.contains("bedwars")) count++;
                    break;
                case SKYWARS:
                    if (wName.contains("skywars")) count++;
                    break;
                case TNTRUN:
                    if (wName.contains("tntrun")) count++;
                    break;
                case MURDER_MYSTERY:
                    if (wName.contains("murder")) count++;
                    break;
                case PROP_HUNT:
                    if (wName.contains("prophunt")) count++;
                    break;
                case THE_BRIDGE_SUMO:
                    if (wName.contains("bridge") || wName.contains("sumo") || wName.contains("duel")) count++;
                    break;
            }
        }
        return count;
    }

    private void startHologramUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllHolograms, 60L, 60L); // every 3s
    }

    public void handlePlayerMove(Player player, Location to) {
        if (to == null || !isInHubWorld(player)) return;

        long now = System.currentTimeMillis();
        Long last = lastTriggerTime.get(player.getUniqueId());
        if (last != null && (now - last) < 2500) {
            return; // Cooldown
        }

        for (PortalData portal : portals) {
            if (portal.contains(to)) {
                lastTriggerTime.put(player.getUniqueId(), now);
                triggerPortalAction(player, portal.type);
                break;
            }
        }
    }

    public void handleHologramClick(Player player, TextDisplay display) {
        if (!isInHubWorld(player)) return;
        for (PortalData portal : portals) {
            if (portal.hologram != null && portal.hologram.getUniqueId().equals(display.getUniqueId())) {
                triggerPortalAction(player, portal.type);
                break;
            }
        }
    }

    private boolean isInHubWorld(Player player) {
        Location spawn = plugin.getLobbyManager().getSpawnLocation();
        return spawn != null && spawn.getWorld() != null && player.getWorld().equals(spawn.getWorld());
    }

    public void triggerPortalAction(Player player, GamePortalType type) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        switch (type) {
            case PLAYER_WORLDS:
                // Opens custom player worlds GUI or teleports
                if (plugin.getPlayerWorldManager() != null) {
                    plugin.getPlayerWorldManager().openPlayerWorldsMenu(player);
                } else {
                    ChatUtil.sendMessage(player, "§a[Portaal] §eJe wordt verbonden met Player Worlds...");
                    player.performCommand("joinworld");
                }
                break;

            case PARKOUR:
                ChatUtil.sendMessage(player, "§6[Portaal] §eJe betreedt Parkour Studio & Duels!");
                player.performCommand("fpixel parkour");
                break;

            case BEDWARS:
                ChatUtil.sendMessage(player, "§c[Portaal] §eJe betreedt BedWars!");
                player.performCommand("join bedwars");
                break;

            case SKYWARS:
                ChatUtil.sendMessage(player, "§b[Portaal] §eJe betreedt SkyWars!");
                player.performCommand("join skywars");
                break;

            case TNTRUN:
                ChatUtil.sendMessage(player, "§e[Portaal] §eJe betreedt TNT Run!");
                player.performCommand("join tntrun");
                break;

            case MURDER_MYSTERY:
                ChatUtil.sendMessage(player, "§d[Portaal] §eJe betreedt Murder Mystery!");
                player.performCommand("join murdermystery");
                break;

            case PROP_HUNT:
                ChatUtil.sendMessage(player, "§3[Portaal] §eJe betreedt Prop Hunt!");
                player.performCommand("join prophunt");
                break;

            case THE_BRIDGE_SUMO:
                ChatUtil.sendMessage(player, "§5[Portaal] §eJe betreedt The Bridge & Sumo 1v1!");
                player.performCommand("duels");
                break;
        }
    }
}
